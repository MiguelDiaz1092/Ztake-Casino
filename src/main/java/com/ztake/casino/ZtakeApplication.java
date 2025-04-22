package com.ztake.casino;

import com.ztake.casino.repository.UserRepository;
import com.ztake.casino.repository.UserRepositoryImpl;
import com.ztake.casino.service.AuthService;
import com.ztake.casino.service.AuthServiceImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicación Ztake Casino.
 */
public class ZtakeApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicializar el repositorio y servicios
        UserRepository userRepository = new UserRepositoryImpl();
        AuthService authService = new AuthServiceImpl(userRepository);

        // Cargar el FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
        Parent root = loader.load();

        // Obtener el controlador y pasarle el servicio
        com.ztake.casino.controller.LoginController controller = loader.getController();
        controller.setAuthService(authService);

        // Configurar la escena
        Scene scene = new Scene(root, 400, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        // Configurar la ventana
        primaryStage.setTitle("Ztake Casino");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}