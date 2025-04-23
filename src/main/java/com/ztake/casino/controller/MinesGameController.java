package com.ztake.casino.controller;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import com.ztake.casino.service.GameService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para el juego Mines con integración de base de datos.
 */
public class MinesGameController {
    private static final Logger LOGGER = Logger.getLogger(MinesGameController.class.getName());

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

    @FXML
    private Slider minesSlider;

    private User currentUser;
    private GameService gameService;
    private GameSession currentGameSession;

    private double currentBet = 5.00;
    private int minesCount = 5;
    private double currentMultiplier = 1.00;
    private boolean gameStarted = false;
    private List<Button> cellButtons = new ArrayList<>();
    private boolean[][] mineLocations = new boolean[4][4]; // true = mina, false = gema

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
        potentialWinningsLabel.setText(String.format("%.2f", currentBet * calculateMultiplier()));

        // Configurar el slider de minas
        if (minesSlider != null) {
            minesSlider.setMin(1);
            minesSlider.setMax(10);
            minesSlider.setValue(minesCount);
            minesSlider.setBlockIncrement(1);
            minesSlider.setMajorTickUnit(1);
            minesSlider.setMinorTickCount(0);
            minesSlider.setShowTickLabels(true);
            minesSlider.setShowTickMarks(true);
            minesSlider.setSnapToTicks(true);

            minesSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                minesCount = newVal.intValue();
                minesCountLabel.setText(String.valueOf(minesCount));
                potentialWinningsLabel.setText(String.format("%.2f", currentBet * calculateMultiplier()));
            });
        }

        // Validación de entrada para apuesta
        betAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                betAmountField.setText(oldVal);
                return;
            }

            try {
                double bet = Double.parseDouble(newVal);
                if (bet > 0) {
                    currentBet = bet;
                    potentialWinningsLabel.setText(String.format("%.2f", currentBet * calculateMultiplier()));
                }
            } catch (NumberFormatException e) {
                // Ignorar
            }
        });

        // Cargar imágenes
        try {
            gemImage = new Image(getClass().getResourceAsStream("/images/gem.png"));
            bombImage = new Image(getClass().getResourceAsStream("/images/bomb.png"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar imágenes: " + e.getMessage());
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
        updateBalanceLabel();
    }

    /**
     * Configura el servicio de juego.
     */
    public void setGameService(GameService gameService) {
        this.gameService = gameService;
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

                // Inicializar posición como segura (sin mina)
                mineLocations[row][col] = false;
            }
        }
    }

    /**
     * Prepara el tablero colocando minas aleatoriamente.
     */
    private void setupMineLocations() {
        // Reiniciar tablero
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                mineLocations[i][j] = false;
            }
        }

        // Colocar minas aleatoriamente
        Random random = new Random();
        int minesPlaced = 0;

        while (minesPlaced < minesCount) {
            int row = random.nextInt(4);
            int col = random.nextInt(4);

            if (!mineLocations[row][col]) {
                mineLocations[row][col] = true;
                minesPlaced++;
            }
        }

        LOGGER.info("Tablero preparado con " + minesCount + " minas");
    }

    /**
     * Maneja el clic en una celda del tablero.
     */
    private void handleCellClick(Button button, int row, int col) {
        if (!gameStarted) {
            try {
                // Iniciar juego con la primera celda
                gameStarted = true;
                setupMineLocations();

                // Deshabilitar slider y campo de apuesta
                if (minesSlider != null) {
                    minesSlider.setDisable(true);
                }
                betAmountField.setDisable(true);

                // Iniciar sesión de juego en la base de datos
                if (gameService != null && currentUser != null) {
                    currentGameSession = gameService.startGame(currentUser, "Mines", currentBet);
                    updateBalanceLabel();
                }

            } catch (IllegalStateException e) {
                // Error por saldo insuficiente
                showAlert(Alert.AlertType.ERROR, "Error", "Saldo insuficiente para realizar esta apuesta.");
                return;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al iniciar juego: " + e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo iniciar el juego: " + e.getMessage());
                return;
            }
        }

        // Verificar si la celda tiene una mina
        boolean isGem = !mineLocations[row][col];

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
            currentMultiplier = calculateMultiplier();
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
     * Calcula el multiplicador según el número de minas y celdas descubiertas.
     */
    private double calculateMultiplier() {
        // Calcular cuántas celdas han sido reveladas
        int revealedCells = 0;
        for (Button button : cellButtons) {
            if (button.isDisabled() && button.getText() != null && !button.getText().equals("💣")) {
                revealedCells++;
            }
        }

        // Fórmula base para el multiplicador: mayor con más minas y más celdas descubiertas
        double baseMult = 1.0 + (minesCount * 0.05) + (revealedCells * 0.1);

        // Redondear a 2 decimales
        return Math.round(baseMult * 100) / 100.0;
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

        // Revelar todas las minas si perdió
        if (!isWin) {
            revealAllMines();
        }

        if (gameService != null && currentGameSession != null) {
            try {
                double winnings = 0;
                if (isWin) {
                    // Calcular ganancias
                    winnings = currentBet * currentMultiplier;
                }

                // Guardar datos del juego para análisis
                JSONObject gameData = new JSONObject();
                gameData.put("minesCount", minesCount);
                gameData.put("multiplier", currentMultiplier);
                gameData.put("revealedCells", countRevealedCells());

                // Finalizar la sesión en la base de datos
                gameService.endGame(
                        currentGameSession,
                        winnings,
                        isWin ? "won" : "lost",
                        gameData.toString()
                );

                // Actualizar la UI
                updateBalanceLabel();

                // Mostrar mensaje según resultado
                if (isWin) {
                    showAlert(Alert.AlertType.INFORMATION, "¡Victoria!",
                            "Has ganado " + String.format("%.2f", winnings));
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "¡Has perdido!",
                            "Has encontrado una mina y perdido tu apuesta de " + String.format("%.2f", currentBet));
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al finalizar el juego: " + e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error al finalizar el juego: " + e.getMessage());
            }
        }

        // Reiniciar juego
        gameStarted = false;
        currentMultiplier = 1.00;
        if (minesSlider != null) {
            minesSlider.setDisable(false);
        }
        betAmountField.setDisable(false);
        initializeGameBoard();
        currentGameSession = null;
    }

    /**
     * Revela todas las minas en el tablero.
     */
    private void revealAllMines() {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (mineLocations[row][col]) {
                    Button button = cellButtons.get(row * 4 + col);
                    if (!button.isDisabled()) {  // Solo si aún no se ha revelado
                        if (bombImage != null) {
                            ImageView bombView = new ImageView(bombImage);
                            bombView.setFitWidth(60);
                            bombView.setFitHeight(60);
                            bombView.setPreserveRatio(true);
                            button.setGraphic(bombView);
                        } else {
                            button.setText("💣");
                        }
                        button.setDisable(true);
                    }
                }
            }
        }
    }

    /**
     * Cuenta cuántas celdas se han revelado.
     */
    private int countRevealedCells() {
        int count = 0;
        for (Button button : cellButtons) {
            if (button.isDisabled()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Actualiza la etiqueta de saldo con el valor actual.
     */
    private void updateBalanceLabel() {
        if (currentUser != null) {
            balanceLabel.setText(String.format("%.2f", currentUser.getBalance()));
        }
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

            // Configurar servicios
            if (gameService != null) {
                mainController.setGameService(gameService);
            }

            // Obtener la ventana actual
            Stage stage = (Stage) cashoutButton.getScene().getWindow();

            // Crear una nueva escena con la vista principal
            stage.getScene().setRoot(mainRoot);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar la vista principal: " + e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la vista principal: " + e.getMessage());
        }
    }

    /**
     * Muestra una alerta con el mensaje especificado.
     */
    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Mines");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}