package com.ztake.casino.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controlador para la vista de historial de juegos.
 */
public class HistoryController {

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

        // Cargar datos de ejemplo
        loadSampleData();
    }

    /**
     * Maneja el evento de clic en el botón de filtro.
     */
    @FXML
    public void handleFilterButtonAction(ActionEvent event) {
        // En una implementación real, aquí se filtrarían los datos según los criterios seleccionados
        // Para esta versión simplificada, simplemente mostramos un mensaje

        String gameFilter = gameFilterComboBox.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Filtro Aplicado");
        alert.setHeaderText(null);
        alert.setContentText("Filtro aplicado:\n" +
                "Juego: " + gameFilter + "\n" +
                "Desde: " + fromDate + "\n" +
                "Hasta: " + toDate);
        alert.showAndWait();

        // Simular que se han filtrado los datos
        updateStatistics();
    }

    /**
     * Carga datos de ejemplo en la tabla de historial.
     */
    private void loadSampleData() {
        ObservableList<GameRecord> data = FXCollections.observableArrayList(
                new GameRecord(1, "Mines", 5.00, 7.25, "Ganado", LocalDateTime.now().minusDays(1)),
                new GameRecord(2, "Mines", 10.00, 0.00, "Perdido", LocalDateTime.now().minusDays(2)),
                new GameRecord(3, "Mines", 7.50, 15.00, "Ganado", LocalDateTime.now().minusDays(3)),
                new GameRecord(4, "Mines", 5.00, 0.00, "Perdido", LocalDateTime.now().minusDays(4)),
                new GameRecord(5, "Mines", 2.00, 3.80, "Ganado", LocalDateTime.now().minusDays(5))
        );

        historyTable.setItems(data);

        // Actualizar estadísticas
        updateStatistics();
    }

    /**
     * Actualiza las estadísticas en base a los datos de la tabla.
     */
    private void updateStatistics() {
        double totalBet = 0;
        double totalWon = 0;
        int gamesPlayed = historyTable.getItems().size();

        for (GameRecord record : historyTable.getItems()) {
            totalBet += record.getBet();
            totalWon += record.getWinnings();
        }

        double netBalance = totalWon - totalBet;

        totalBetLabel.setText(String.format("%.2f", totalBet));
        totalWonLabel.setText(String.format("%.2f", totalWon));
        netBalanceLabel.setText(String.format("%.2f", netBalance));
        gamesPlayedLabel.setText(String.valueOf(gamesPlayed));
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