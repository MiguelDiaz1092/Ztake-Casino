package com.ztake.casino.controller;

import com.ztake.casino.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controlador para la vista principal del dashboard.
 */
public class MainController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Button homeButton;

    @FXML
    private Button gamesButton;

    @FXML
    private Button historyButton;

    @FXML
    private Button profileButton;

    @FXML
    private Button supportButton;

    @FXML
    private Button logoutButton;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox homeView;

    @FXML
    private TableView<?> recentActivityTable;

    private User currentUser;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Aquí se inicializarían los datos de la tabla de actividad reciente
        // Por ahora dejamos la tabla vacía
    }

    /**
     * Configura el usuario actual para mostrar sus datos.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Actualizar la UI con los datos del usuario
        usernameLabel.setText(user.getUsername());
        balanceLabel.setText(String.format("%.2f", user.getBalance()));
    }

    /**
     * Maneja el evento de clic en el botón de inicio.
     */
    @FXML
    public void handleHomeButtonAction(ActionEvent event) {
        // Simplemente mostramos la vista de inicio
        homeView.setVisible(true);

        // Si hubiera otras vistas cargadas, las ocultaríamos aquí
    }

    /**
     * Maneja el evento de clic en el botón de juegos.
     * En esta versión simplificada no hace nada.
     */
    @FXML
    public void handleGamesButtonAction(ActionEvent event) {
        // Por simplicidad, no implementamos esta funcionalidad
        showAlert("Información", "Lista de juegos", "Esta funcionalidad se implementará más adelante.");
    }

    /**
     * Maneja el evento de clic en el botón de historial.
     * Carga la vista de historial.
     */
    @FXML
    public void handleHistoryButtonAction(ActionEvent event) {
        try {
            // Cargar la vista de historial
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history-view.fxml"));
            Parent historyView = loader.load();

            // Obtener el controlador
            HistoryController historyController = loader.getController();

            // Limpiar el área de contenido y agregar la vista
            contentArea.getChildren().clear();
            contentArea.getChildren().add(historyView);

            // Ocultar la vista de inicio
            homeView.setVisible(false);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Error de carga", "No se pudo cargar la vista de historial.");
        }
    }

    /**
     * Maneja el evento de clic en el botón de perfil.
     * Carga la vista de perfil.
     */
    @FXML
    public void handleProfileButtonAction(ActionEvent event) {
        try {
            // Cargar la vista de perfil
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile-view.fxml"));
            Parent profileView = loader.load();

            // Obtener el controlador
            ProfileController profileController = loader.getController();

            // Configurar el controlador con los datos del usuario si es necesario
            if (currentUser != null) {
                profileController.setUserData(currentUser);
            }

            // Limpiar el área de contenido y agregar la vista
            contentArea.getChildren().clear();
            contentArea.getChildren().add(profileView);

            // Ocultar la vista de inicio
            homeView.setVisible(false);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Error de carga", "No se pudo cargar la vista de perfil.");
        }
    }

    /**
     * Maneja el evento de clic en el botón de soporte.
     * Carga la vista de soporte.
     */
    @FXML
    public void handleSupportButtonAction(ActionEvent event) {
        try {
            // Cargar la vista de soporte
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/support-view.fxml"));
            Parent supportView = loader.load();

            // Limpiar el área de contenido y agregar la vista
            contentArea.getChildren().clear();
            contentArea.getChildren().add(supportView);

            // Ocultar la vista de inicio
            homeView.setVisible(false);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Error de carga", "No se pudo cargar la vista de soporte.");
        }
    }

    /**
     * Maneja el evento de clic en el botón de cerrar sesión.
     */
    @FXML
    public void handleLogoutButtonAction(ActionEvent event) {
        // Mostrar confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Sesión");
        alert.setHeaderText(null);
        alert.setContentText("¿Estás seguro de que quieres cerrar sesión?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                // Cargar la vista de login
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
                Parent loginRoot = loader.load();

                // Obtener la ventana actual
                Stage stage = (Stage) logoutButton.getScene().getWindow();

                // Crear una nueva escena con la vista de login
                stage.getScene().setRoot(loginRoot);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Error de carga", "No se pudo cargar la vista de login.");
            }
        }
    }

    /**
     * Maneja el evento de clic en la tarjeta del juego Mines.
     */
    @FXML
    public void handleMinesGameAction(MouseEvent event) {
        try {
            // Cargar la vista del juego Mines
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mines-game-view.fxml"));
            Parent gameRoot = loader.load();

            // Obtener el controlador
            MinesGameController gameController = loader.getController();

            // Configurar el controlador con los datos del usuario
            if (currentUser != null) {
                gameController.setCurrentUser(currentUser);
            }

            // Obtener la ventana actual
            Stage stage = (Stage) homeButton.getScene().getWindow();

            // Crear una nueva escena con la vista del juego
            stage.getScene().setRoot(gameRoot);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Error de carga", "No se pudo cargar el juego Mines.");
        }
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