package com.ztake.casino.repository;

import com.ztake.casino.config.DatabaseConfig;
import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación del repositorio de sesiones de juego utilizando JPA.
 */
public class GameSessionRepositoryImpl implements GameSessionRepository {
    private static final Logger LOGGER = Logger.getLogger(GameSessionRepositoryImpl.class.getName());

    @Override
    public GameSession save(GameSession gameSession) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();

            if (gameSession.getId() == null) {
                // Es una nueva sesión
                if (gameSession.getSessionDate() == null) {
                    gameSession.setSessionDate(LocalDateTime.now());
                }
                em.persist(gameSession);
            } else {
                // Es una sesión existente
                gameSession = em.merge(gameSession);
            }

            em.getTransaction().commit();
            return gameSession;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al guardar sesión de juego", e);
            throw new RuntimeException("No se pudo guardar la sesión de juego", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<GameSession> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return Optional.ofNullable(em.find(GameSession.class, id));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar sesión de juego por ID", e);
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    @Override
    public List<GameSession> findByUser(User user) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<GameSession> query = em.createQuery(
                    "SELECT g FROM GameSession g WHERE g.user = :user ORDER BY g.sessionDate DESC",
                    GameSession.class);
            query.setParameter("user", user);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar sesiones de juego por usuario", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<GameSession> findByUserAndDateRange(User user, LocalDateTime fromDate, LocalDateTime toDate) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<GameSession> query = em.createQuery(
                    "SELECT g FROM GameSession g WHERE g.user = :user AND g.sessionDate BETWEEN :fromDate AND :toDate ORDER BY g.sessionDate DESC",
                    GameSession.class);
            query.setParameter("user", user);
            query.setParameter("fromDate", fromDate);
            query.setParameter("toDate", toDate);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar sesiones de juego por usuario y rango de fechas", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<GameSession> findByUserAndGameType(User user, String gameType) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<GameSession> query = em.createQuery(
                    "SELECT g FROM GameSession g WHERE g.user = :user AND g.gameType = :gameType ORDER BY g.sessionDate DESC",
                    GameSession.class);
            query.setParameter("user", user);
            query.setParameter("gameType", gameType);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar sesiones de juego por usuario y tipo de juego", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<GameSession> findByUserAndGameTypeAndDateRange(User user, String gameType, LocalDateTime fromDate, LocalDateTime toDate) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<GameSession> query = em.createQuery(
                    "SELECT g FROM GameSession g WHERE g.user = :user AND g.gameType = :gameType AND g.sessionDate BETWEEN :fromDate AND :toDate ORDER BY g.sessionDate DESC",
                    GameSession.class);
            query.setParameter("user", user);
            query.setParameter("gameType", gameType);
            query.setParameter("fromDate", fromDate);
            query.setParameter("toDate", toDate);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar sesiones de juego por usuario, tipo de juego y rango de fechas", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            GameSession gameSession = em.find(GameSession.class, id);
            if (gameSession != null) {
                em.remove(gameSession);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al eliminar sesión de juego", e);
            throw new RuntimeException("No se pudo eliminar la sesión de juego", e);
        } finally {
            em.close();
        }
    }
}