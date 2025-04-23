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

            // Obtener el usuario autenticado
            User authenticatedUser = userOptional.get();

            // Mostrar mensaje de bienvenida
            showSuccessAlert(owner, "Bienvenido a Ztake Casino, " + authenticatedUser.getUsername() + "!");

            // Registrar en consola
            System.out.println("Autenticación exitosa para: " + authenticatedUser.getUsername());

            // IMPORTANTE: Navegar a la vista principal del dashboard
            try {
                // Cargar la vista principal
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
                Parent mainView = loader.load();

                // Obtener el controlador y pasarle el usuario autenticado
                MainController mainController = loader.getController();
                mainController.setCurrentUser(authenticatedUser);

                // Crear una nueva escena
                Scene scene = new Scene(mainView, 1000, 700);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

                // Obtener el stage actual y cambiar la escena
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Ztake Casino - Dashboard");
                stage.centerOnScreen(); // Centrar la ventana

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error al cargar la vista principal: " + e.getMessage());

                // Mostrar mensaje de error
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error de navegación");
                alert.setContentText("No se pudo cargar la vista principal: " + e.getMessage());
                alert.showAndWait();
            }

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
     * Carga la vista de registro y la muestra.
     */
    @FXML
    public void handleRegisterAction(ActionEvent event) {
        try {
            // Cargar la vista de registro
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register-view.fxml"));
            Parent registerRoot = loader.load();

            // Obtener el controlador y pasarle el servicio
            RegisterController registerController = loader.getController();
            registerController.setAuthService(authService);

            // Crear y configurar la escena
            Scene scene = new Scene(registerRoot, 400, 650);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // Obtener el stage actual y cambiar la escena
            Stage stage = (Stage) registerLink.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo cargar la vista de registro: " + e.getMessage());
            alert.showAndWait();
        }
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