package com.ztake.casino.controller;

import com.ztake.casino.model.User;
import com.ztake.casino.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Controlador para la vista de registro.
 */
public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button registerButton;

    @FXML
    private Hyperlink loginLink;

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
     * Maneja el evento de clic en el botón de registro.
     */
    @FXML
    public void handleRegisterButtonAction(ActionEvent event) {
        // Obtener la ventana actual
        Window owner = registerButton.getScene().getWindow();

        // Obtener datos del formulario
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validar campos
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("Por favor, completa todos los campos");
            errorLabel.setVisible(true);
            return;
        }

        // Validar email
        if (!isValidEmail(email)) {
            errorLabel.setText("Por favor, ingresa un email válido");
            errorLabel.setVisible(true);
            return;
        }

        // Validar que las contraseñas coinciden
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Las contraseñas no coinciden");
            errorLabel.setVisible(true);
            return;
        }

        try {
            // Intentar registrar al usuario
            User newUser = authService.register(username, email, password);

            // Registro exitoso
            errorLabel.setVisible(false);
            showSuccessAlert(owner, "Cuenta creada exitosamente. ¡Bienvenido a Ztake Casino, " + newUser.getUsername() + "!");

            // Redirigir al login
            navigateToLogin();

        } catch (IllegalArgumentException e) {
            // Error durante el registro (usuario o email ya existe)
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    /**
     * Maneja el evento de clic en el enlace de login.
     */
    @FXML
    public void handleLoginLinkAction(ActionEvent event) {
        navigateToLogin();
    }

    /**
     * Navega a la vista de login.
     */
    private void navigateToLogin() {
        try {
            // Cargar la vista de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent loginRoot = loader.load();

            // Obtener el controlador y pasarle el servicio
            LoginController loginController = loader.getController();
            loginController.setAuthService(authService);

            // Crear y configurar la escena
            Scene scene = new Scene(loginRoot, 400, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // Obtener el stage actual y cambiar la escena
            Stage stage = (Stage) loginLink.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo cargar la vista de login: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Muestra una alerta de éxito.
     */
    private void showSuccessAlert(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registro Exitoso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    /**
     * Valida si un email tiene formato correcto.
     */
    private boolean isValidEmail(String email) {
        // Validación simple de formato de email
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }
}