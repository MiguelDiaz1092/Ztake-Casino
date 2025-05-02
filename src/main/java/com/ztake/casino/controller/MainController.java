package com.ztake.casino.controller;

import com.ztake.casino.ZtakeApplication;
import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import com.ztake.casino.service.GameService;
import javafx.application.Platform;
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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador mejorado para la vista principal del dashboard.
 */
public class MainController {
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML
    private Label usernameLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label clockLabel;

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
    private AtomicBoolean componentsInitialized = new AtomicBoolean(false);
    private Timeline clockTimeline;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Verificar que los componentes de UI existan
        if (usernameLabel == null || balanceLabel == null) {
            LOGGER.severe("Error: Componentes de UI no inicializados correctamente");
            return;
        }

        // Inicializar valores por defecto
        usernameLabel.setText("Usuario no conectado");
        balanceLabel.setText("0.00");

        // Iniciar el reloj si el label existe
        if (clockLabel != null) {
            startClock();
        }

        // Configurar columnas de la tabla de actividad reciente
        if (recentActivityTable != null && gameColumn != null && betColumn != null &&
                resultColumn != null && dateColumn != null) {
            try {
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

                componentsInitialized.set(true);
                LOGGER.info("Componentes de tabla de actividad reciente inicializados correctamente");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al configurar la tabla de actividad reciente", e);
            }
        } else {
            LOGGER.warning("La tabla de actividad reciente o sus columnas no están disponibles en el FXML");
        }
    }

    /**
     * Inicia el reloj en tiempo real.
     */
    private void startClock() {
        if (clockLabel == null) return;

        // Actualizar la hora inmediatamente
        updateClock();

        // Crear un Timeline que actualice la hora cada segundo
        clockTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateClock())
        );
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * Actualiza el reloj con la hora actual.
     */
    private void updateClock() {
        if (clockLabel == null) return;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        clockLabel.setText(now.format(formatter));
    }

    /**
     * Configura el usuario actual para mostrar sus datos.
     */
    public void setCurrentUser(User user) {
        if (user == null) {
            LOGGER.warning("Intento de establecer un usuario nulo");
            return;
        }

        this.currentUser = user;

        // Actualizar la UI con los datos del usuario
        Platform.runLater(() -> {
            try {
                if (usernameLabel != null) {
                    usernameLabel.setText(user.getUsername());
                }

                updateBalanceLabel();

                // Cargar actividad reciente si ya está disponible el servicio
                if (gameService != null) {
                    // Intentar cargar actividad reciente, pero solo si los componentes ya están inicializados
                    if (componentsInitialized.get()) {
                        loadRecentActivity();
                    } else {
                        LOGGER.info("Componentes no inicializados, se pospondrá la carga de actividad reciente");
                        // Intentar cargar después de un breve retraso para dar tiempo a la inicialización
                        new Thread(() -> {
                            try {
                                Thread.sleep(500);
                                Platform.runLater(() -> {
                                    if (componentsInitialized.get()) {
                                        loadRecentActivity();
                                    }
                                });
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al actualizar la UI con los datos del usuario", e);
            }
        });
    }

    /**
     * Configura el servicio de juego.
     */
    public void setGameService(GameService gameService) {
        if (gameService == null) {
            // Si es nulo, intentar obtener el servicio global
            gameService = ZtakeApplication.getGameService();
            if (gameService == null) {
                LOGGER.warning("No se pudo obtener el servicio de juego");
                return;
            }
        }

        this.gameService = gameService;
        LOGGER.info("Servicio de juego configurado correctamente");

        // Cargar actividad reciente si ya está disponible el usuario
        Platform.runLater(() -> {
            if (currentUser != null && componentsInitialized.get()) {
                loadRecentActivity();
            }
        });
    }

    /**
     * Carga la actividad reciente del usuario desde la base de datos.
     */
    private void loadRecentActivity() {
        if (!componentsInitialized.get() || recentActivityTable == null || gameService == null || currentUser == null) {
            LOGGER.warning("No se puede cargar la actividad reciente: componentes no inicializados");
            return;
        }

        try {
            // Obtener las últimas sesiones de juego
            List<GameSession> recentSessions = gameService.getUserGameHistory(currentUser);

            // Verificar que hay sesiones
            if (recentSessions.isEmpty()) {
                LOGGER.info("No hay sesiones de juego recientes para el usuario: " + currentUser.getUsername());
                return;
            }

            // Limitar a las 5 más recientes
            if (recentSessions.size() > 5) {
                recentSessions = recentSessions.subList(0, 5);
            }

            // Limpiar la tabla antes de agregar nuevos elementos
            recentActivityTable.getItems().clear();

            // Convertir a objetos para la tabla
            for (GameSession session : recentSessions) {
                recentActivityTable.getItems().add(new RecentGameRecord(
                        session.getGameType(),
                        session.getBetAmount(),
                        translateResult(session.getResult()),
                        session.getSessionDate()
                ));
            }

            LOGGER.info("Actividad reciente cargada exitosamente: " + recentSessions.size() + " sesiones");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar actividad reciente: " + e.getMessage(), e);
        }
    }

    /**
     * Traduce el resultado de la base de datos a un formato más amigable para mostrar.
     */
    private String translateResult(String dbResult) {
        if (dbResult == null) {
            return "Desconocido";
        }

        switch (dbResult) {
            case "won": return "Ganado";
            case "lost": return "Perdido";
            case "in_progress": return "En progreso";
            default: return dbResult;
        }
    }

    /**
     * Actualiza la etiqueta de saldo con el valor actual.
     */
    private void updateBalanceLabel() {
        if (balanceLabel != null && currentUser != null) {
            try {
                balanceLabel.setText(String.format("%.2f", currentUser.getBalance()));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al actualizar etiqueta de saldo", e);
                balanceLabel.setText("Error");
            }
        } else if (balanceLabel != null) {
            balanceLabel.setText("0.00");
        }
    }

    // ... otros métodos aquí ...

    /**
     * Maneja el evento de clic en la tarjeta del juego Mines.
     */
    @FXML
    public void handleMinesGameAction(MouseEvent event) {
        if (homeButton == null) {
            LOGGER.severe("Error: componentes de UI no inicializados correctamente");
            return;
        }

        try {
            // Asegurarnos de tener un servicio de juego
            if (gameService == null) {
                gameService = ZtakeApplication.getGameService();
                if (gameService == null) {
                    LOGGER.severe("Error: No hay servicio de juego disponible");
                    showAlert("Error", "Error de configuración", "No se pudo iniciar el juego debido a un problema de configuración.");
                    return;
                }
            }

            // Cargar la vista del juego Mines
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mines-game-view.fxml"));
            Parent gameRoot = loader.load();

            // Obtener el controlador
            MinesGameController gameController = loader.getController();

            // Configurar el controlador con los datos necesarios
            if (currentUser != null) {
                gameController.setCurrentUser(currentUser);
                LOGGER.info("Usuario establecido para el controlador de Mines: " + currentUser.getUsername());
            } else {
                LOGGER.warning("No hay usuario actual para pasar al controlador del juego");
            }

            // Establecer el servicio de juego - IMPORTANTE
            gameController.setGameService(gameService);
            LOGGER.info("Servicio de juego establecido para el controlador de Mines");

            // Obtener la ventana actual
            Stage stage = (Stage) homeButton.getScene().getWindow();

            // Verificar que tenemos una ventana válida
            if (stage != null && stage.getScene() != null) {
                // Crear una nueva escena con la vista del juego
                stage.getScene().setRoot(gameRoot);
                LOGGER.info("Navegación exitosa a la vista de Mines");
            } else {
                LOGGER.severe("Error: No se pudo acceder a la ventana o escena actual");
                throw new IllegalStateException("No se pudo acceder a la ventana actual");
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el juego Mines: " + e.getMessage(), e);
            showAlert("Error", "Error de carga", "No se pudo cargar el juego Mines: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al navegar al juego Mines: " + e.getMessage(), e);
            showAlert("Error", "Error inesperado", "Ocurrió un error al intentar iniciar el juego: " + e.getMessage());
        }
    }

    /**
     * Muestra una alerta con el mensaje especificado.
     */
    private void showAlert(String title, String header, String content) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al mostrar alerta: " + e.getMessage(), e);
        }
    }

    // ... resto del código ...

    /**
     * Detiene el reloj cuando se deja de usar la vista.
     */
    public void dispose() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
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
            this.game = game != null ? game : "Desconocido";
            this.bet = bet != null ? bet : 0.0;
            this.result = result != null ? result : "Desconocido";

            // Formatear fecha con manejo de nulos
            if (dateTime != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                this.date = dateTime.format(formatter);
            } else {
                this.date = "Fecha desconocida";
            }
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