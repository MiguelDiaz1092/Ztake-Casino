package com.ztake.casino.repository;

import com.ztake.casino.config.DatabaseConfig;
import com.ztake.casino.model.SupportTicket;
import com.ztake.casino.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación del repositorio de tickets de soporte utilizando JPA.
 */
public class SupportTicketRepositoryImpl implements SupportTicketRepository {
    private static final Logger LOGGER = Logger.getLogger(SupportTicketRepositoryImpl.class.getName());

    @Override
    public SupportTicket save(SupportTicket ticket) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();

            if (ticket.getId() == null) {
                // Es un nuevo ticket
                if (ticket.getCreatedDate() == null) {
                    ticket.setCreatedDate(LocalDateTime.now());
                }
                if (ticket.getLastUpdatedDate() == null) {
                    ticket.setLastUpdatedDate(LocalDateTime.now());
                }
                if (ticket.getStatus() == null) {
                    ticket.setStatus("open");
                }
                em.persist(ticket);
            } else {
                // Es un ticket existente
                ticket.setLastUpdatedDate(LocalDateTime.now());
                ticket = em.merge(ticket);
            }

            em.getTransaction().commit();
            return ticket;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al guardar ticket de soporte", e);
            throw new RuntimeException("No se pudo guardar el ticket de soporte", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<SupportTicket> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return Optional.ofNullable(em.find(SupportTicket.class, id));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar ticket de soporte por ID", e);
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    @Override
    public List<SupportTicket> findByUser(User user) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<SupportTicket> query = em.createQuery(
                    "SELECT t FROM SupportTicket t WHERE t.user = :user ORDER BY t.lastUpdatedDate DESC",
                    SupportTicket.class);
            query.setParameter("user", user);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar tickets de soporte por usuario", e);
            return List.of();
        } finally {
            em.close();
        }
    }






    @Override
    public List<SupportTicket> findByStatus(String status) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<SupportTicket> query = em.createQuery(
                    "SELECT t FROM SupportTicket t WHERE t.status = :status ORDER BY t.lastUpdatedDate DESC",
                    SupportTicket.class);
            query.setParameter("status", status);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar tickets de soporte por estado", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<SupportTicket> findByUserAndStatus(User user, String status) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<SupportTicket> query = em.createQuery(
                    "SELECT t FROM SupportTicket t WHERE t.user = :user AND t.status = :status ORDER BY t.lastUpdatedDate DESC",
                    SupportTicket.class);
            query.setParameter("user", user);
            query.setParameter("status", status);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar tickets de soporte por usuario y estado", e);
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
            SupportTicket ticket = em.find(SupportTicket.class, id);
            if (ticket != null) {
                em.remove(ticket);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al eliminar ticket de soporte", e);
            throw new RuntimeException("No se pudo eliminar el ticket de soporte", e);
        } finally {
            em.close();
        }
    }
}