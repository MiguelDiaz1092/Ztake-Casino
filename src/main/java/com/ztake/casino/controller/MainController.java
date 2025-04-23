package com.ztake.casino.controller;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import com.ztake.casino.service.GameService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la vista principal del dashboard con integración de base de datos.
 */
public class MainController {
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

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
    private TableView<RecentGameRecord> recentActivityTable;

    @FXML
    private TableColumn<RecentGameRecord, String> gameColumn;

    @FXML
    private TableColumn<RecentGameRecord, Double> betColumn;

    @FXML
    private TableColumn<RecentGameRecord, String> resultColumn;

    @FXML
    private TableColumn<RecentGameRecord, String> dateColumn;

    private User currentUser;
    private GameService gameService;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Configurar columnas de la tabla de actividad reciente
        if (recentActivityTable != null) {
            gameColumn.setCellValueFactory(new PropertyValueFactory<>("game"));
            betColumn.setCellValueFactory(new PropertyValueFactory<>("bet"));
            resultColumn.setCellValueFactory(new PropertyValueFactory<>("result"));
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

            // Aplicar formato para moneda
            betColumn.setCellFactory(col -> new TableCell<RecentGameRecord, Double>() {
                @Override
                protected void updateItem(Double amount, boolean empty) {
                    super.updateItem(amount, empty);
                    if (empty || amount == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", amount));
                    }
                }
            });

            // Aplicar estilos a la columna de resultados
            resultColumn.setCellFactory(col -> new TableCell<RecentGameRecord, String>() {
                @Override
                protected void updateItem(String result, boolean empty) {
                    super.updateItem(result, empty);
                    if (empty || result == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(result);
                        if ("Ganado".equals(result)) {
                            setStyle("-fx-text-fill: #2ECC71;"); // Verde para ganado
                        } else if ("Perdido".equals(result)) {
                            setStyle("-fx-text-fill: #E74C3C;"); // Rojo para perdido
                        } else {
                            setStyle("");
                        }
                    }
                }
            });
        }
    }

    /**
     * Configura el usuario actual para mostrar sus datos.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Actualizar la UI con los datos del usuario
        usernameLabel.setText(user.getUsername());
        balanceLabel.setText(String.format("%.2f", user.getBalance()));

        // Cargar actividad reciente si ya está disponible el servicio
        if (gameService != null) {
            loadRecentActivity();
        }
    }

    /**
     * Configura el servicio de juego.
     */
    public void setGameService(GameService gameService) {
        this.gameService = gameService;

        // Cargar actividad reciente si ya está disponible el usuario
        if (currentUser != null) {
            loadRecentActivity();
        }
    }

    /**
     * Carga la actividad reciente del usuario desde la base de datos.
     */
    private void loadRecentActivity() {
        if (recentActivityTable != null && gameService != null && currentUser != null) {
            try {
                // Obtener las últimas 5 sesiones de juego
                List<GameSession> recentSessions = gameService.getUserGameHistory(currentUser);

                // Limitar a las 5 más recientes
                if (recentSessions.size() > 5) {
                    recentSessions = recentSessions.subList(0, 5);
                }

                // Convertir a objetos para la tabla
                recentActivityTable.getItems().clear();
                for (GameSession session : recentSessions) {
                    recentActivityTable.getItems().add(new RecentGameRecord(
                            session.getGameType(),
                            session.getBetAmount(),
                            translateResult(session.getResult()),
                            session.getSessionDate()
                    ));
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cargar actividad reciente: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Traduce el resultado de la base de datos a un formato más amigable para mostrar.
     */
    private String translateResult(String dbResult) {
        if ("won".equals(dbResult)) {
            return "Ganado";
        } else if ("lost".equals(dbResult)) {
            return "Perdido";
        } else {
            return dbResult;
        }
    }

    /**
     * Maneja el evento de clic en el botón de inicio.
     */
    @FXML
    public void handleHomeButtonAction(ActionEvent event) {
        // Simplemente mostramos la vista de inicio
        homeView.setVisible(true);

        // Recargar actividad reciente
        loadRecentActivity();

        // Ocultar otras vistas
        for (int i = 0; i < contentArea.getChildren().size(); i++) {
            contentArea.getChildren().get(i).setVisible(false);
        }
    }

    /**
     * Maneja el evento de clic en el botón de juegos.
     */
    @FXML
    public void handleGamesButtonAction(ActionEvent event) {
        try {
            // Cargar la vista de juegos
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/games-menu-view.fxml"));
            Parent gamesView = loader.load();

            // Obtener el controlador
            GamesMenuController gamesController = loader.getController();

            // Configurar el controlador con los datos necesarios
            if (currentUser != null) {
                gamesController.setCurrentUser(currentUser);
            }
            if (gameService != null) {
                gamesController.setGameService(gameService);
            }

            // Limpiar el área de contenido y agregar la vista
            contentArea.getChildren().clear();
            contentArea.getChildren().add(gamesView);

            // Ocultar la vista de inicio
            homeView.setVisible(false);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar la vista de juegos: " + e.getMessage(), e);
            showAlert("Error", "Error de carga", "No se pudo cargar la vista de juegos.");
        }
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

            // Configurar el controlador con los datos necesarios
            if (gameService != null) {
                historyController.setGameService(gameService);
            }
            if (currentUser != null) {
                historyController.setCurrentUser(currentUser);
            }

            // Limpiar el área de contenido y agregar la vista
            contentArea.getChildren().clear();
            contentArea.getChildren().add(historyView);

            // Ocultar la vista de inicio
            homeView.setVisible(false);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar la vista de historial: " + e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, "Error al cargar la vista de perfil: " + e.getMessage(), e);
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

            // Obtener el controlador
            SupportController supportController = loader.getController();

            // Configurar el controlador con los datos necesarios
            if (currentUser != null) {
                supportController.setCurrentUser(currentUser);
            }

            // Limpiar el área de contenido y agregar la vista
            contentArea.getChildren().clear();
            contentArea.getChildren().add(supportView);

            // Ocultar la vista de inicio
            homeView.setVisible(false);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar la vista de soporte: " + e.getMessage(), e);
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

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Cargar la vista de login
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
                Parent loginRoot = loader.load();

                // Obtener la ventana actual
                Stage stage = (Stage) logoutButton.getScene().getWindow();

                // Crear una nueva escena con la vista de login
                stage.getScene().setRoot(loginRoot);

                LOGGER.info("Usuario cerró sesión: " + (currentUser != null ? currentUser.getUsername() : "desconocido"));

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error al cargar la vista de login: " + e.getMessage(), e);
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

            // Configurar el controlador con los datos necesarios
            if (currentUser != null) {
                gameController.setCurrentUser(currentUser);
            }
            if (gameService != null) {
                gameController.setGameService(gameService);
            }

            // Obtener la ventana actual
            Stage stage = (Stage) homeButton.getScene().getWindow();

            // Crear una nueva escena con la vista del juego
            stage.getScene().setRoot(gameRoot);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el juego Mines: " + e.getMessage(), e);
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

    /**
     * Clase para representar un registro en la tabla de actividad reciente.
     */
    public static class RecentGameRecord {
        private final String game;
        private final Double bet;
        private final String result;
        private final String date;

        public RecentGameRecord(String game, Double bet, String result, LocalDateTime dateTime) {
            this.game = game;
            this.bet = bet;
            this.result = result;

            // Formatear fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            this.date = dateTime.format(formatter);
        }

        public String getGame() {
            return game;
        }

        public Double getBet() {
            return bet;
        }

        public String getResult() {
            return result;
        }

        public String getDate() {
            return date;
        }
    }
}