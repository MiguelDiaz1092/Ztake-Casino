package com.ztake.casino.controller;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import com.ztake.casino.service.GameService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controlador para la vista de historial de juegos con integración de base de datos.
 */
public class HistoryController {
    private static final Logger LOGGER = Logger.getLogger(HistoryController.class.getName());

    @FXML
    private ComboBox<String> gameFilterComboBox;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private Button filterButton;

    @FXML
    private TableView<GameRecord> historyTable;

    @FXML
    private TableColumn<GameRecord, Integer> idColumn;

    @FXML
    private TableColumn<GameRecord, String> gameColumn;

    @FXML
    private TableColumn<GameRecord, Double> betColumn;

    @FXML
    private TableColumn<GameRecord, Double> winningsColumn;

    @FXML
    private TableColumn<GameRecord, String> resultColumn;

    @FXML
    private TableColumn<GameRecord, String> dateColumn;

    @FXML
    private Label totalBetLabel;

    @FXML
    private Label totalWonLabel;

    @FXML
    private Label netBalanceLabel;

    @FXML
    private Label gamesPlayedLabel;

    @FXML
    private Label winRateLabel;

    private User currentUser;
    private GameService gameService;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Configurar las opciones del ComboBox de filtro
        gameFilterComboBox.setItems(FXCollections.observableArrayList(
                "Todos los juegos", "Mines", "Slots", "Ruleta", "Blackjack"
        ));
        gameFilterComboBox.getSelectionModel().selectFirst();

        // Configurar las columnas de la tabla
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        gameColumn.setCellValueFactory(new PropertyValueFactory<>("game"));
        betColumn.setCellValueFactory(new PropertyValueFactory<>("bet"));
        winningsColumn.setCellValueFactory(new PropertyValueFactory<>("winnings"));
        resultColumn.setCellValueFactory(new PropertyValueFactory<>("result"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Configurar DatePickers
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());

        // Aplicar formato de moneda a las columnas numéricas
        betColumn.setCellFactory(col -> new TableCell<GameRecord, Double>() {
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

        winningsColumn.setCellFactory(col -> new TableCell<GameRecord, Double>() {
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
        resultColumn.setCellFactory(col -> new TableCell<GameRecord, String>() {
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

    /**
     * Configura el usuario actual para mostrar su historial.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (gameService != null) {
            loadGameHistory(); // Cargar historial al establecer el usuario
        }
    }

    /**
     * Configura el servicio de juego.
     */
    public void setGameService(GameService gameService) {
        this.gameService = gameService;
        if (currentUser != null) {
            loadGameHistory(); // Cargar historial al establecer el servicio
        }
    }

    /**
     * Maneja el evento de clic en el botón de filtro.
     */
    @FXML
    public void handleFilterButtonAction(ActionEvent event) {
        if (gameService == null || currentUser == null) {
            showAlert("Error de filtro", "No se puede aplicar el filtro: servicio o usuario no inicializado.");
            return;
        }

        loadGameHistory(); // Cargar historial con los filtros aplicados
    }

    /**
     * Carga el historial de juegos desde la base de datos con los filtros aplicados.
     */
    private void loadGameHistory() {
        try {
            // Obtener filtros
            String gameFilter = gameFilterComboBox.getValue();
            LocalDate fromDate = fromDatePicker.getValue();
            LocalDate toDate = toDatePicker.getValue();

            // Convertir fechas a LocalDateTime para incluir todo el día
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);

            // Determinar el tipo de juego a filtrar (null si es "Todos los juegos")
            String gameType = null;
            if (gameFilter != null && !gameFilter.equals("Todos los juegos")) {
                gameType = gameFilter;
            }

            // Obtener historial de juegos filtrando por tipo y fecha
            List<GameSession> gameSessions = gameService.getUserGameHistory(
                    currentUser, gameType, fromDateTime, toDateTime);

            // Convertir a GameRecord para la tabla
            ObservableList<GameRecord> data = FXCollections.observableArrayList();
            for (GameSession session : gameSessions) {
                data.add(new GameRecord(
                        session.getId().intValue(),
                        session.getGameType(),
                        session.getBetAmount(),
                        session.getWinningAmount(),
                        translateResult(session.getResult()),
                        session.getSessionDate()
                ));
            }

            // Mostrar en la tabla
            historyTable.setItems(data);

            // Calcular y mostrar estadísticas
            updateStatistics();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar historial de juegos: " + e.getMessage(), e);
            showAlert("Error", "No se pudo cargar el historial de juegos: " + e.getMessage());
        }
    }

    /**
     * Actualiza las estadísticas en base a los datos de la tabla.
     */
    private void updateStatistics() {
        if (gameService != null && currentUser != null) {
            try {
                // Obtener estadísticas de juego del servicio
                Map<String, Object> stats = gameService.calculateUserGameStats(currentUser);

                // Mostrar estadísticas en la UI
                totalBetLabel.setText(String.format("%.2f", stats.get("totalBet")));
                totalWonLabel.setText(String.format("%.2f", stats.get("totalWon")));
                netBalanceLabel.setText(String.format("%.2f", stats.get("netBalance")));
                gamesPlayedLabel.setText(String.valueOf(stats.get("gamesPlayed")));

                if (winRateLabel != null) {
                    winRateLabel.setText(String.format("%.1f%%", stats.get("winRate")));
                }

                // Aplicar estilo al balance neto según sea positivo o negativo
                double netBalance = (double) stats.get("netBalance");
                if (netBalance > 0) {
                    netBalanceLabel.setStyle("-fx-text-fill: #2ECC71;"); // Verde para positivo
                } else if (netBalance < 0) {
                    netBalanceLabel.setStyle("-fx-text-fill: #E74C3C;"); // Rojo para negativo
                } else {
                    netBalanceLabel.setStyle(""); // Estilo normal para cero
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al calcular estadísticas: " + e.getMessage(), e);
            }
        } else {
            // Si no hay servicio disponible, calcular estadísticas básicas desde la tabla
            calculateBasicStats();
        }
    }

    /**
     * Calcula estadísticas básicas desde los datos de la tabla.
     */
    private void calculateBasicStats() {
        double totalBet = 0;
        double totalWon = 0;
        int gamesPlayed = historyTable.getItems().size();
        int gamesWon = 0;

        for (GameRecord record : historyTable.getItems()) {
            totalBet += record.getBet();
            totalWon += record.getWinnings();
            if ("Ganado".equals(record.getResult())) {
                gamesWon++;
            }
        }

        double netBalance = totalWon - totalBet;
        double winRate = gamesPlayed > 0 ? (double) gamesWon / gamesPlayed * 100 : 0;

        totalBetLabel.setText(String.format("%.2f", totalBet));
        totalWonLabel.setText(String.format("%.2f", totalWon));
        netBalanceLabel.setText(String.format("%.2f", netBalance));
        gamesPlayedLabel.setText(String.valueOf(gamesPlayed));

        if (winRateLabel != null) {
            winRateLabel.setText(String.format("%.1f%%", winRate));
        }

        // Aplicar estilo al balance neto
        if (netBalance > 0) {
            netBalanceLabel.setStyle("-fx-text-fill: #2ECC71;");
        } else if (netBalance < 0) {
            netBalanceLabel.setStyle("-fx-text-fill: #E74C3C;");
        } else {
            netBalanceLabel.setStyle("");
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
     * Muestra una alerta con el mensaje especificado.
     */
    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Historial");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Clase interna para representar un registro de juego en la tabla.
     */
    public static class GameRecord {
        private final Integer id;
        private final String game;
        private final Double bet;
        private final Double winnings;
        private final String result;
        private final String date;

        public GameRecord(Integer id, String game, Double bet, Double winnings, String result, LocalDateTime dateTime) {
            this.id = id;
            this.game = game;
            this.bet = bet;
            this.winnings = winnings;
            this.result = result;

            // Formatear fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            this.date = dateTime.format(formatter);
        }

        public Integer getId() {
            return id;
        }

        public String getGame() {
            return game;
        }

        public Double getBet() {
            return bet;
        }

        public Double getWinnings() {
            return winnings;
        }

        public String getResult() {
            return result;
        }

        public String getDate() {
            return date;
        }
    }
}