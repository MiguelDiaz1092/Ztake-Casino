package com.ztake.casino.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuración mejorada de la base de datos y gestión de la conexión
 */
public class DatabaseConfig {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final String PERSISTENCE_UNIT_NAME = "ZtakeCasinoPU";
    private static EntityManagerFactory emf;
    private static boolean initialized = false;

    /**
     * Inicializa la conexión a la base de datos de manera segura.
     * Se llama automáticamente cuando se necesita un EntityManager.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            Properties dbProps = loadProperties();
            initEntityManagerFactory(dbProps);
            initialized = true;
            LOGGER.info("Base de datos inicializada correctamente");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inicializando la base de datos: " + e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }

    private static void initEntityManagerFactory(Properties dbProps) {
        try {
            Map<String, Object> configOverrides = new HashMap<>();

            // Configurar las propiedades de conexión
            configOverrides.put("jakarta.persistence.jdbc.driver", dbProps.getProperty("jakarta.persistence.jdbc.driver"));
            configOverrides.put("jakarta.persistence.jdbc.url", dbProps.getProperty("jakarta.persistence.jdbc.url"));
            configOverrides.put("jakarta.persistence.jdbc.user", dbProps.getProperty("jakarta.persistence.jdbc.user"));
            configOverrides.put("jakarta.persistence.jdbc.password", dbProps.getProperty("jakarta.persistence.jdbc.password"));

            // Configurar otras propiedades de Hibernate
            configOverrides.put("hibernate.dialect", dbProps.getProperty("hibernate.dialect"));
            configOverrides.put("hibernate.show_sql", dbProps.getProperty("hibernate.show_sql"));
            configOverrides.put("hibernate.format_sql", dbProps.getProperty("hibernate.format_sql"));
            configOverrides.put("hibernate.hbm2ddl.auto", dbProps.getProperty("hibernate.hbm2ddl.auto"));

            // Verificar si HikariCP está disponible
            try {
                Class.forName("com.zaxxer.hikari.hibernate.HikariConnectionProvider");

                // Configuración básica de HikariCP
                configOverrides.put("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
                configOverrides.put("hibernate.hikari.maximumPoolSize", "10");
                configOverrides.put("hibernate.hikari.minimumIdle", "2");
                configOverrides.put("hibernate.hikari.idleTimeout", "30000");

                LOGGER.info("HikariCP disponible y configurado");
            } catch (ClassNotFoundException e) {
                LOGGER.warning("HikariCP no está disponible en el classpath. Usando el pool de conexiones predeterminado.");
            }

            emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, configOverrides);
            LOGGER.info("EntityManagerFactory inicializada correctamente");
        } catch (Exception e) {
            LOGGER.severe("Error inicializando EntityManagerFactory: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar EntityManagerFactory", e);
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (in == null) {
                throw new IOException("No se pudo encontrar el archivo database.properties");
            }
            props.load(in);
        }
        return props;
    }

    /**
     * Obtiene un EntityManager para operaciones de base de datos.
     * Inicializa la conexión si es necesario.
     *
     * @return EntityManager conectado a la base de datos
     */
    public static EntityManager getEntityManager() {
        if (!initialized) {
            initialize();
        }

        if (emf == null || !emf.isOpen()) {
            throw new IllegalStateException("EntityManagerFactory no está inicializada o está cerrada");
        }

        return emf.createEntityManager();
    }

    /**
     * Cierra los recursos de la base de datos al finalizar la aplicación
     */
    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            try {
                emf.close();
                initialized = false;
                LOGGER.info("Recursos de base de datos cerrados correctamente");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cerrar los recursos de base de datos", e);
            }
        }
    }
}