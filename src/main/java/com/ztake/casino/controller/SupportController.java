package com.ztake.casino.controller;

import com.ztake.casino.model.SupportTicket;
import com.ztake.casino.model.User;
import com.ztake.casino.repository.SupportTicketRepository;
import com.ztake.casino.repository.SupportTicketRepositoryImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la vista de soporte con integración de base de datos.
 */
public class SupportController {
    private static final Logger LOGGER = Logger.getLogger(SupportController.class.getName());

    @FXML
    private TextField subjectField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextArea messageArea;

    @FXML
    private Label formMessageLabel;

    @FXML
    private Button sendButton;

    @FXML
    private Accordion faqAccordion;

    @FXML
    private TableView<TicketRecord> ticketsTable;

    @FXML
    private VBox ticketsView;

    private User currentUser;
    private SupportTicketRepository supportTicketRepository;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Inicializar el repositorio
        supportTicketRepository = new SupportTicketRepositoryImpl();

        // Configurar las categorías disponibles
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Problema técnico",
                "Consulta sobre juegos",
                "Problema con mi cuenta",
                "Depósitos y retiros",
                "Otro"
        ));

        // Ocultar el mensaje inicialmente
        formMessageLabel.setVisible(false);

        // Configurar tabla de tickets si existe
        if (ticketsTable != null) {
            // Configurar columnas
            TableColumn<TicketRecord, String> subjectColumn = new TableColumn<>("Asunto");
            subjectColumn.setCellValueFactory(cellData -> cellData.getValue().subjectProperty());

            TableColumn<TicketRecord, String> categoryColumn = new TableColumn<>("Categoría");
            categoryColumn.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());

            TableColumn<TicketRecord, String> statusColumn = new TableColumn<>("Estado");
            statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

            TableColumn<TicketRecord, String> dateColumn = new TableColumn<>("Fecha");
            dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

            ticketsTable.getColumns().addAll(subjectColumn, categoryColumn, statusColumn, dateColumn);

            // Configurar evento de selección
            ticketsTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                            showTicketDetails(newSelection.getId());
                        }
                    });
        }
    }

    /**
     * Configura el usuario actual.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Cargar tickets del usuario si existe la vista
        if (ticketsView != null) {
            loadUserTickets();
        }
    }

    /**
     * Maneja el evento de clic en el botón de enviar consulta.
     */
    @FXML
    public void handleSendButtonAction(ActionEvent event) {
        // Validar campos
        if (subjectField.getText().isEmpty() ||
                categoryComboBox.getValue() == null ||
                messageArea.getText().isEmpty()) {

            showMessage("Por favor, completa todos los campos del formulario.", false);
            return;
        }

        try {
            // Crear y guardar el ticket
            if (currentUser != null && supportTicketRepository != null) {
                SupportTicket ticket = new SupportTicket(
                        currentUser,
                        subjectField.getText(),
                        categoryComboBox.getValue(),
                        messageArea.getText()
                );

                supportTicketRepository.save(ticket);

                // Mostrar mensaje de éxito
                showMessage("Tu consulta ha sido enviada. Te responderemos lo antes posible.", true);

                // Limpiar el formulario
                subjectField.clear();
                categoryComboBox.getSelectionModel().clearSelection();
                messageArea.clear();

                // Actualizar la lista de tickets si existe
                if (ticketsView != null) {
                    loadUserTickets();
                }

                LOGGER.info("Ticket de soporte creado por usuario: " + currentUser.getUsername());
            } else {
                showMessage("Error: No se pudo procesar la solicitud. Por favor, inténtalo de nuevo.", false);
                LOGGER.warning("Intento de crear ticket sin usuario o repositorio inicializado");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al guardar ticket de soporte: " + e.getMessage(), e);
            showMessage("Error al procesar la solicitud: " + e.getMessage(), false);
        }
    }

    /**
     * Carga los tickets del usuario actual.
     */
    private void loadUserTickets() {
        if (ticketsTable != null && currentUser != null && supportTicketRepository != null) {
            try {
                List<SupportTicket> tickets = supportTicketRepository.findByUser(currentUser);

                ObservableList<TicketRecord> ticketRecords = FXCollections.observableArrayList();
                for (SupportTicket ticket : tickets) {
                    ticketRecords.add(new TicketRecord(
                            ticket.getId(),
                            ticket.getSubject(),
                            ticket.getCategory(),
                            translateStatus(ticket.getStatus()),
                            ticket.getCreatedDate().toString()
                    ));
                }

                ticketsTable.setItems(ticketRecords);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cargar tickets de soporte: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Muestra los detalles de un ticket específico.
     */
    private void showTicketDetails(Long ticketId) {
        if (supportTicketRepository != null) {
            try {
                supportTicketRepository.findById(ticketId).ifPresent(ticket -> {
                    // Crear y mostrar diálogo con detalles
                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("Detalles del Ticket");
                    dialog.setHeaderText("Ticket #" + ticket.getId());

                    // Contenido
                    VBox content = new VBox(10);

                    Label subjectLabel = new Label("Asunto: " + ticket.getSubject());
                    Label categoryLabel = new Label("Categoría: " + ticket.getCategory());
                    Label statusLabel = new Label("Estado: " + translateStatus(ticket.getStatus()));
                    Label dateLabel = new Label("Fecha: " + ticket.getCreatedDate());

                    TextArea messageView = new TextArea(ticket.getMessage());
                    messageView.setEditable(false);
                    messageView.setPrefHeight(150);

                    Label notesLabel = new Label("Respuesta del administrador:");
                    TextArea notesView = new TextArea(ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "Sin respuesta aún");
                    notesView.setEditable(false);
                    notesView.setPrefHeight(100);

                    content.getChildren().addAll(
                            subjectLabel, categoryLabel, statusLabel, dateLabel,
                            new Label("Tu mensaje:"), messageView,
                            notesLabel, notesView
                    );

                    dialog.getDialogPane().setContent(content);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

                    dialog.showAndWait();
                });

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cargar detalles del ticket: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Traduce el estado del ticket a formato amigable.
     */
    private String translateStatus(String status) {
        switch (status) {
            case "open": return "Abierto";
            case "in_progress": return "En proceso";
            case "resolved": return "Resuelto";
            case "closed": return "Cerrado";
            default: return status;
        }
    }

    /**
     * Muestra un mensaje en la interfaz.
     * @param message el mensaje a mostrar
     * @param isSuccess true si es un mensaje de éxito, false si es un error
     */
    private void showMessage(String message, boolean isSuccess) {
        formMessageLabel.setText(message);

        // Aplicar estilo según el tipo de mensaje
        formMessageLabel.getStyleClass().removeAll("success", "error");
        if (isSuccess) {
            formMessageLabel.getStyleClass().add("success");
        } else {
            formMessageLabel.getStyleClass().add("error");
        }

        formMessageLabel.setVisible(true);
    }

    /**
     * Clase para representar un ticket en la tabla.
     */
    public static class TicketRecord {
        private final Long id;
        private final javafx.beans.property.SimpleStringProperty subject;
        private final javafx.beans.property.SimpleStringProperty category;
        private final javafx.beans.property.SimpleStringProperty status;
        private final javafx.beans.property.SimpleStringProperty date;

        public TicketRecord(Long id, String subject, String category, String status, String date) {
            this.id = id;
            this.subject = new javafx.beans.property.SimpleStringProperty(subject);
            this.category = new javafx.beans.property.SimpleStringProperty(category);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
            this.date = new javafx.beans.property.SimpleStringProperty(date);
        }

        public Long getId() {
            return id;
        }

        public javafx.beans.property.StringProperty subjectProperty() {
            return subject;
        }

        public javafx.beans.property.StringProperty categoryProperty() {
            return category;
        }

        public javafx.beans.property.StringProperty statusProperty() {
            return status;
        }

        public javafx.beans.property.StringProperty dateProperty() {
            return date;
        }
    }
}