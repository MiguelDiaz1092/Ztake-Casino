package com.ztake.casino;

import com.ztake.casino.config.DatabaseConfig;
import com.ztake.casino.repository.*;
import com.ztake.casino.service.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Clase principal de la aplicación Ztake Casino.
 */
public class ZtakeApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger(ZtakeApplication.class.getName());

    // Mantener referencias globales a los servicios
    private static AuthService authService;
    private static GameService gameService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicializar la base de datos
        DatabaseConfig.initialize();

        // Inicializar el repositorio y servicios
        UserRepository userRepository = new UserRepositoryImpl();
        GameSessionRepository gameSessionRepository = new GameSessionRepositoryImpl();
        TransactionRepository transactionRepository = new TransactionRepositoryImpl();
        SupportTicketRepository supportTicketRepository = new SupportTicketRepositoryImpl();

        // Inicializar usuarios de prueba
        ((UserRepositoryImpl) userRepository).initializeTestUsers();

        // Inicializar servicios
        authService = new AuthServiceImpl(userRepository);
        gameService = new GameServiceImpl(gameSessionRepository, transactionRepository, userRepository);

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

        LOGGER.info("Aplicación iniciada correctamente");
    }

    @Override
    public void stop() {
        // Cerrar recursos al finalizar la aplicación
        try {
            DatabaseConfig.shutdown();
            LOGGER.info("Aplicación cerrada correctamente");
        } catch (Exception e) {
            LOGGER.severe("Error al cerrar la aplicación: " + e.getMessage());
        } finally {
            Platform.exit();
        }
    }

    /**
     * Obtiene el servicio de autenticación.
     * @return el servicio de autenticación
     */
    public static AuthService getAuthService() {
        return authService;
    }

    /**
     * Obtiene el servicio de juego.
     * @return el servicio de juego
     */
    public static GameService getGameService() {
        return gameService;
    }

    public static void main(String[] args) {
        launch(args);
    }
}