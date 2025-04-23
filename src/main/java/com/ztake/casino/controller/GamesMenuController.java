package com.ztake.casino.controller;

import com.ztake.casino.model.User;
import com.ztake.casino.service.GameService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para el menú de juegos.
 */
public class GamesMenuController {
    private static final Logger LOGGER = Logger.getLogger(GamesMenuController.class.getName());

    @FXML
    private GridPane gamesGrid;

    @FXML
    private VBox minesCard;

    @FXML
    private VBox slotsCard;

    @FXML
    private VBox rouletteCard;

    @FXML
    private VBox blackjackCard;

    @FXML
    private Label balanceLabel;

    private User currentUser;
    private GameService gameService;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Configurar eventos de clic para las tarjetas de juegos
        if (minesCard != null) {
            minesCard.setOnMouseClicked(this::handleMinesGameAction);
        }

        if (slotsCard != null) {
            slotsCard.setOnMouseClicked(this::handleComingSoonAction);
        }

        if (rouletteCard != null) {
            rouletteCard.setOnMouseClicked(this::handleComingSoonAction);
        }

        if (blackjackCard != null) {
            blackjackCard.setOnMouseClicked(this::handleComingSoonAction);
        }
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
     * Actualiza la etiqueta del saldo.
     */
    private void updateBalanceLabel() {
        if (balanceLabel != null && currentUser != null) {
            balanceLabel.setText(String.format("%.2f", currentUser.getBalance()));
        }
    }

    /**
     * Maneja el evento de clic en la tarjeta del juego Mines.
     */
    private void handleMinesGameAction(MouseEvent event) {
        try {
            // Cargar la vista del juego Mines
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mines-game-view.fxml"));
            Parent gameRoot = loader.load();

            // Obtener el controlador
            MinesGameController gameController = loader.getController();

            // Configurar el controlador con los datos necesarios
            if (currentUser != null) {
                gameController.setCurrentUser(currentUser);
            }
            if (gameService != null) {
                gameController.setGameService(gameService);
            }

            // Obtener la ventana actual
            Stage stage = (Stage) minesCard.getScene().getWindow();

            // Crear una nueva escena con la vista del juego
            stage.getScene().setRoot(gameRoot);

            LOGGER.info("Usuario " + currentUser.getUsername() + " abrió el juego Mines");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el juego Mines: " + e.getMessage(), e);
            showAlert("Error", "Error de carga", "No se pudo cargar el juego Mines.");
        }
    }

    /**
     * Maneja el evento de clic en juegos que aún no están disponibles.
     */
    private void handleComingSoonAction(MouseEvent event) {
        showAlert("Próximamente", "Esta función estará disponible pronto",
                "Estamos trabajando en traerte nuevos y emocionantes juegos. ¡Vuelve pronto!");
    }

    /**
     * Muestra una alerta con el mensaje especificado.
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}