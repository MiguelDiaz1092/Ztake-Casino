package com.ztake.casino.controller;

import com.ztake.casino.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controlador para la vista de perfil de usuario.
 */
public class ProfileController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Label registrationDateLabel;

    @FXML
    private Label currentBalanceLabel;

    @FXML
    private Label totalBetLabel;

    @FXML
    private Label totalWonLabel;

    private User currentUser;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Ocultar el mensaje inicialmente
        messageLabel.setVisible(false);

        // Deshabilitar los campos hasta que se cargue un usuario
        usernameField.setDisable(true);
        emailField.setDisable(true);
    }

    /**
     * Configura los datos del usuario en la vista.
     */
    public void setUserData(User user) {
        this.currentUser = user;

        // Configurar campos del formulario
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());

        // Habilitar los campos
        usernameField.setDisable(false);
        emailField.setDisable(false);

        // Configurar información adicional
        registrationDateLabel.setText("01/01/2023"); // Fecha ficticia para demo
        currentBalanceLabel.setText(String.format("%.2f", user.getBalance()));

        // Valores ficticios para las estadísticas
        totalBetLabel.setText("500.00");
        totalWonLabel.setText("750.00");
    }

    /**
     * Maneja el evento de clic en el botón de guardar cambios.
     */
    @FXML
    public void handleSaveButtonAction(ActionEvent event) {
        // Validar campos
        if (usernameField.getText().isEmpty() || emailField.getText().isEmpty()) {
            showMessage("Por favor, completa todos los campos obligatorios.", false);
            return;
        }

        // Validar contraseñas
        if (!currentPasswordField.getText().isEmpty()) {
            // Si se está intentando cambiar la contraseña
            if (newPasswordField.getText().isEmpty() || confirmPasswordField.getText().isEmpty()) {
                showMessage("Por favor, completa todos los campos de contraseña.", false);
                return;
            }

            if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
                showMessage("Las contraseñas nuevas no coinciden.", false);
                return;
            }

            // Validar contraseña actual (en un sistema real se verificaría contra la base de datos)
            if (!currentPasswordField.getText().equals(currentUser.getPassword())) {
                showMessage("La contraseña actual es incorrecta.", false);
                return;
            }

            // Actualizar contraseña
            currentUser.setPassword(newPasswordField.getText());
        }

        // Actualizar datos del usuario
        currentUser.setUsername(usernameField.getText());
        currentUser.setEmail(emailField.getText());

        // Mostrar mensaje de éxito
        showMessage("Datos actualizados correctamente.", true);

        // Limpiar campos de contraseña
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    /**
     * Muestra un mensaje en la interfaz.
     * @param message el mensaje a mostrar
     * @param isSuccess true si es un mensaje de éxito, false si es un error
     */
    private void showMessage(String message, boolean isSuccess) {
        messageLabel.setText(message);

        // Aplicar estilo según el tipo de mensaje
        messageLabel.getStyleClass().removeAll("success", "error");
        if (isSuccess) {
            messageLabel.getStyleClass().add("success");
        } else {
            messageLabel.getStyleClass().add("error");
        }

        messageLabel.setVisible(true);
    }
}