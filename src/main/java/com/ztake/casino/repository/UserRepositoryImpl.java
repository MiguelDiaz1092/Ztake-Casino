package com.ztake.casino.repository;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.ztake.casino.config.DatabaseConfig;
import com.ztake.casino.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación mejorada del repositorio de usuarios utilizando JPA.
 */
public class UserRepositoryImpl implements UserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepositoryImpl.class.getName());

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        try (EntityManager em = DatabaseConfig.getEntityManager()) {
            try {
                TypedQuery<User> query = em.createQuery(
                        "SELECT u FROM User u WHERE u.username = :credential OR u.email = :credential",
                        User.class);
                query.setParameter("credential", usernameOrEmail);
                return Optional.ofNullable(query.getSingleResult());
            } catch (NoResultException e) {
                return Optional.empty();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al buscar usuario por username o email: " + usernameOrEmail, e);
                return Optional.empty();
            }
        }
    }

    @Override
    public User save(User user) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction transaction = null;

        try {
            transaction = em.getTransaction();
            transaction.begin();

            if (user.getId() == null) {
                // Es un nuevo usuario
                if (user.getRegistrationDate() == null) {
                    user.setRegistrationDate(LocalDateTime.now());
                }
                if (user.getStatus() == null) {
                    user.setStatus("active");
                }
                em.persist(user);
            } else {
                // Es un usuario existente
                user = em.merge(user);
            }

            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al guardar usuario: " + user.getUsername(), e);
            throw new RuntimeException("No se pudo guardar el usuario", e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<User> findAll() {
        try (EntityManager em = DatabaseConfig.getEntityManager()) {
            try {
                return em.createQuery("SELECT u FROM User u", User.class).getResultList();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al obtener todos los usuarios", e);
                return List.of();
            }
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try (EntityManager em = DatabaseConfig.getEntityManager()) {
            try {
                return Optional.ofNullable(em.find(User.class, id));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al buscar usuario por ID: " + id, e);
                return Optional.empty();
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction transaction = null;

        try {
            transaction = em.getTransaction();
            transaction.begin();
            User user = em.find(User.class, id);
            if (user != null) {
                em.remove(user);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al eliminar usuario con ID: " + id, e);
            throw new RuntimeException("No se pudo eliminar el usuario", e);
        } finally {
            em.close();
        }
    }

    /**
     * Crea usuarios de prueba iniciales si no existen
     */
    public void initializeTestUsers() {
        LOGGER.info("Verificando usuarios de prueba...");

        // Verificar si ya existe un usuario administrador
        if (findByUsernameOrEmail("admin").isPresent() || findByUsernameOrEmail("admin@ztake.com").isPresent()) {
            LOGGER.info("Usuario admin ya existe, omitiendo creación");
        } else {
            // Crear admin con contraseña hasheada
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@ztake.com");

            // Hash de la contraseña con BCrypt
            String hashedAdminPassword = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray());
            admin.setPassword(hashedAdminPassword);

            admin.setBalance(10000.0);
            save(admin);
            LOGGER.info("Usuario admin creado correctamente");
        }

        // Verificar si ya existe un usuario normal
        if (findByUsernameOrEmail("user").isPresent() || findByUsernameOrEmail("user@ztake.com").isPresent()) {
            LOGGER.info("Usuario user ya existe, omitiendo creación");
        } else {
            // Crear usuario normal con contraseña hasheada
            User user = new User();
            user.setUsername("user");
            user.setEmail("user@ztake.com");

            // Hash de la contraseña con BCrypt
            String hashedUserPassword = BCrypt.withDefaults().hashToString(12, "user123".toCharArray());
            user.setPassword(hashedUserPassword);

            user.setBalance(1000.0);
            save(user);
            LOGGER.info("Usuario user creado correctamente");
        }
    }
}