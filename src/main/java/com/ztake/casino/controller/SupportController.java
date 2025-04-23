package com.ztake.casino.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controlador para la vista de soporte.
 */
public class SupportController {

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

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
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

        // En un sistema real, aquí se enviaría la consulta al servidor
        // Para esta versión simplificada, solo mostramos un mensaje de éxito

        // Mostrar mensaje de éxito
        showMessage("Tu consulta ha sido enviada. Te responderemos lo antes posible.", true);

        // Limpiar el formulario
        subjectField.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        messageArea.clear();
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
}