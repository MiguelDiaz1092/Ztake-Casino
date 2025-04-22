package com.ztake.casino.controller;

import com.ztake.casino.model.User;
import com.ztake.casino.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Controlador para la vista de login.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox rememberMeCheckbox;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Hyperlink registerLink;

    private AuthService authService;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Ocultar el mensaje de error inicialmente
        errorLabel.setVisible(false);
    }

    /**
     * Configura el servicio de autenticación.
     */
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Maneja el evento de clic en el botón de login.
     */
    @FXML
    public void handleLoginButtonAction(ActionEvent event) {
        // Obtener la ventana actual
        Window owner = loginButton.getScene().getWindow();

        // Obtener datos del formulario
        String usernameOrEmail = usernameField.getText();
        String password = passwordField.getText();

        // Validar campos
        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Por favor, completa todos los campos");
            errorLabel.setVisible(true);
            return;
        }

        // Intentar autenticar al usuario
        Optional<User> userOptional = authService.authenticate(usernameOrEmail, password);

        if (userOptional.isPresent()) {
            // Autenticación exitosa
            errorLabel.setVisible(false);
            showSuccessAlert(owner, "Bienvenido a Ztake Casino, " + userOptional.get().getUsername() + "!");

            // Aquí deberías abrir la ventana principal del casino
            // Por ahora, simplemente lo registramos
            System.out.println("Autenticación exitosa para: " + userOptional.get().getUsername());
        } else {
            // Autenticación fallida
            errorLabel.setText("Usuario o contraseña incorrectos");
            errorLabel.setVisible(true);
        }
    }

    /**
     * Maneja el evento de clic en el enlace de olvido de contraseña.
     */
    @FXML
    public void handleForgotPasswordAction(ActionEvent event) {
        Window owner = forgotPasswordLink.getScene().getWindow();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Recuperar contraseña");
        dialog.setHeaderText("Por favor, ingresa tu correo electrónico");
        dialog.setContentText("Email:");
        dialog.initOwner(owner);

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(email -> {
            boolean sent = authService.recoverPassword(email);

            if (sent) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Recuperación de contraseña");
                alert.setHeaderText(null);
                alert.setContentText("Se ha enviado un correo con instrucciones para recuperar tu contraseña.");
                alert.initOwner(owner);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("No se encontró ninguna cuenta con ese correo electrónico.");
                alert.initOwner(owner);
                alert.showAndWait();
            }
        });
    }

    /**
     * Maneja el evento de clic en el enlace de registro.
     */
    @FXML
    public void handleRegisterAction(ActionEvent event) {
        Window owner = registerLink.getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registro");
        alert.setHeaderText(null);
        alert.setContentText("La funcionalidad de registro aún no está implementada.");
        alert.initOwner(owner);
        alert.showAndWait();
    }

    /**
     * Muestra una alerta de éxito.
     */
    private void showSuccessAlert(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Login Exitoso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.showAndWait();
    }
}