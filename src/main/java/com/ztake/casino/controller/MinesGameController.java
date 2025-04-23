package com.ztake.casino.controller;

import com.ztake.casino.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Controlador para el juego Mines.
 */
public class MinesGameController {

    @FXML
    private Label balanceLabel;

    @FXML
    private TextField betAmountField;

    @FXML
    private Label minesCountLabel;

    @FXML
    private GridPane gameBoard;

    @FXML
    private Button cashoutButton;

    @FXML
    private Label potentialWinningsLabel;

    private User currentUser;
    private double currentBet = 5.00;
    private int minesCount = 5;
    private double currentMultiplier = 1.00;
    private boolean gameStarted = false;
    private List<Button> cellButtons = new ArrayList<>();

    // Imágenes para las celdas
    private Image gemImage;
    private Image bombImage;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Inicializar valores predeterminados
        betAmountField.setText(String.format("%.2f", currentBet));
        minesCountLabel.setText(String.valueOf(minesCount));
        potentialWinningsLabel.setText(String.format("%.2f", currentBet * 1.27));

        // Cargar imágenes
        try {
            gemImage = new Image(getClass().getResourceAsStream("/images/gem.png"));
            bombImage = new Image(getClass().getResourceAsStream("/images/bomb.png"));
        } catch (Exception e) {
            System.out.println("Error al cargar imágenes: " + e.getMessage());
            // Continuar sin imágenes si no se pueden cargar
        }

        // Inicializar el tablero
        initializeGameBoard();
    }

    /**
     * Configura el usuario actual para mostrar su saldo.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        balanceLabel.setText(String.format("%.2f", user.getBalance()));
    }

    /**
     * Inicializa el tablero de juego con botones.
     */
    private void initializeGameBoard() {
        // Limpiar el tablero y la lista de botones
        gameBoard.getChildren().clear();
        cellButtons.clear();

        // Crear un tablero 4x4
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                Button cellButton = new Button();
                cellButton.getStyleClass().add("game-cell");

                // Evento de clic para revelar celda
                final int r = row;
                final int c = col;
                cellButton.setOnAction(e -> handleCellClick(cellButton, r, c));

                // Agregar a la cuadrícula y a la lista
                gameBoard.add(cellButton, col, row);
                cellButtons.add(cellButton);
            }
        }
    }

    /**
     * Maneja el clic en una celda del tablero.
     */
    private void handleCellClick(Button button, int row, int col) {
        if (!gameStarted) {
            // Iniciar juego con la primera celda
            gameStarted = true;

            // Deshabilitar el campo de apuesta
            betAmountField.setDisable(true);

            // Actualizar saldo
            if (currentUser != null) {
                currentUser.setBalance(currentUser.getBalance() - currentBet);
                balanceLabel.setText(String.format("%.2f", currentUser.getBalance()));
            }
        }

        // Simular si es gema o bomba (70% probabilidad de gema)
        Random random = new Random();
        boolean isGem = random.nextDouble() > 0.3;

        if (isGem) {
            // Es una gema
            if (gemImage != null) {
                ImageView gemView = new ImageView(gemImage);
                gemView.setFitWidth(60);
                gemView.setFitHeight(60);
                gemView.setPreserveRatio(true);
                button.setGraphic(gemView);
            } else {
                // Si no hay imagen, usar texto
                button.setText("💎");
            }

            // Aumentar multiplicador
            currentMultiplier += 0.2;
            potentialWinningsLabel.setText(String.format("%.2f", currentBet * currentMultiplier));

        } else {
            // Es una bomba
            if (bombImage != null) {
                ImageView bombView = new ImageView(bombImage);
                bombView.setFitWidth(60);
                bombView.setFitHeight(60);
                bombView.setPreserveRatio(true);
                button.setGraphic(bombView);
            } else {
                // Si no hay imagen, usar texto
                button.setText("💣");
            }

            // Juego perdido
            endGame(false);
        }

        // Deshabilitar el botón para que no se pueda volver a hacer clic
        button.setDisable(true);
    }

    /**
     * Maneja el evento de clic en el botón SALIR (cashout).
     */
    @FXML
    public void handleCashoutButtonAction(ActionEvent event) {
        if (gameStarted) {
            // Finalizar el juego con victoria
            endGame(true);
        } else {
            // Si no ha comenzado, volver al dashboard
            navigateToDashboard();
        }
    }

    /**
     * Finaliza el juego actual.
     * @param isWin true si el jugador ganó, false si perdió
     */
    private void endGame(boolean isWin) {
        // Deshabilitar todas las celdas
        for (Button button : cellButtons) {
            button.setDisable(true);
        }

        if (isWin && currentUser != null) {
            // Sumar ganancias al saldo
            double winnings = currentBet * currentMultiplier;
            currentUser.setBalance(currentUser.getBalance() + winnings);
            balanceLabel.setText(String.format("%.2f", currentUser.getBalance()));

            // Mostrar mensaje de victoria
            showAlert("¡Victoria!", "Has ganado " + String.format("%.2f", winnings));
        } else if (!isWin) {
            // Mostrar mensaje de derrota
            showAlert("¡Has perdido!", "Has encontrado una mina y perdido tu apuesta de " + String.format("%.2f", currentBet));
        }

        // Reiniciar juego
        gameStarted = false;
        currentMultiplier = 1.00;
        betAmountField.setDisable(false);
        initializeGameBoard();
    }

    /**
     * Navega de vuelta al dashboard principal.
     */
    private void navigateToDashboard() {
        try {
            // Cargar la vista principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            Parent mainRoot = loader.load();

            // Obtener el controlador
            MainController mainController = loader.getController();

            // Configurar el controlador con los datos del usuario
            if (currentUser != null) {
                mainController.setCurrentUser(currentUser);
            }

            // Obtener la ventana actual
            Stage stage = (Stage) cashoutButton.getScene().getWindow();

            // Crear una nueva escena con la vista principal
            stage.getScene().setRoot(mainRoot);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo cargar la vista principal.");
        }
    }

    /**
     * Muestra una alerta con el mensaje especificado.
     */
    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mines");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}