package com.ztake.casino.repository;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.ztake.casino.config.DatabaseConfig;
import com.ztake.casino.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación del repositorio de usuarios utilizando JPA.
 */
public class UserRepositoryImpl implements UserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepositoryImpl.class.getName());

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :credential OR u.email = :credential",
                    User.class);
            query.setParameter("credential", usernameOrEmail);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar usuario por username o email", e);
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    @Override
    public User save(User user) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();

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

            em.getTransaction().commit();
            return user;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al guardar usuario", e);
            throw new RuntimeException("No se pudo guardar el usuario", e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<User> findAll() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u", User.class).getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todos los usuarios", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return Optional.ofNullable(em.find(User.class, id));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar usuario por ID", e);
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, id);
            if (user != null) {
                em.remove(user);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al eliminar usuario", e);
            throw new RuntimeException("No se pudo eliminar el usuario", e);
        } finally {
            em.close();
        }
    }

    /**
     * Crea usuarios de prueba iniciales si no existen
     */
    public void initializeTestUsers() {
        // Verificar si ya existen usuarios
        if (!findAll().isEmpty()) {
            return;
        }

        LOGGER.info("Creando usuarios de prueba iniciales...");

        // Crear admin con contraseña hasheada
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@ztake.com");

        // Hash de la contraseña con BCrypt
        String hashedAdminPassword = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray());
        admin.setPassword(hashedAdminPassword);

        admin.setBalance(10000.0);
        save(admin);

        // Crear usuario normal con contraseña hasheada
        User user = new User();
        user.setUsername("user");
        user.setEmail("user@ztake.com");

        // Hash de la contraseña con BCrypt
        String hashedUserPassword = BCrypt.withDefaults().hashToString(12, "user123".toCharArray());
        user.setPassword(hashedUserPassword);

        user.setBalance(1000.0);
        save(user);

        LOGGER.info("Usuarios de prueba creados correctamente");
    }
}