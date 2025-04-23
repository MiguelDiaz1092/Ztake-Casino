package com.ztake.casino.repository;

import com.ztake.casino.config.DatabaseConfig;
import com.ztake.casino.model.Transaction;
import com.ztake.casino.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación del repositorio de transacciones utilizando JPA.
 */
public class TransactionRepositoryImpl implements TransactionRepository {
    private static final Logger LOGGER = Logger.getLogger(TransactionRepositoryImpl.class.getName());

    @Override
    public Transaction save(Transaction transaction) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();

            if (transaction.getId() == null) {
                // Es una nueva transacción
                if (transaction.getTransactionDate() == null) {
                    transaction.setTransactionDate(LocalDateTime.now());
                }
                if (transaction.getStatus() == null) {
                    transaction.setStatus("pending");
                }
                em.persist(transaction);
            } else {
                // Es una transacción existente
                transaction = em.merge(transaction);
            }

            em.getTransaction().commit();
            return transaction;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al guardar transacción", e);
            throw new RuntimeException("No se pudo guardar la transacción", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Transaction.class, id));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar transacción por ID", e);
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Transaction> findByUser(User user) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Transaction> query = em.createQuery(
                    "SELECT t FROM Transaction t WHERE t.user = :user ORDER BY t.transactionDate DESC",
                    Transaction.class);
            query.setParameter("user", user);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar transacciones por usuario", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Transaction> findByUserAndDateRange(User user, LocalDateTime fromDate, LocalDateTime toDate) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Transaction> query = em.createQuery(
                    "SELECT t FROM Transaction t WHERE t.user = :user AND t.transactionDate BETWEEN :fromDate AND :toDate ORDER BY t.transactionDate DESC",
                    Transaction.class);
            query.setParameter("user", user);
            query.setParameter("fromDate", fromDate);
            query.setParameter("toDate", toDate);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar transacciones por usuario y rango de fechas", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Transaction> findByUserAndType(User user, String transactionType) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Transaction> query = em.createQuery(
                    "SELECT t FROM Transaction t WHERE t.user = :user AND t.transactionType = :type ORDER BY t.transactionDate DESC",
                    Transaction.class);
            query.setParameter("user", user);
            query.setParameter("type", transactionType);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar transacciones por usuario y tipo", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Transaction> findPendingTransactions() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Transaction> query = em.createQuery(
                    "SELECT t FROM Transaction t WHERE t.status = 'pending' ORDER BY t.transactionDate ASC",
                    Transaction.class);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al buscar transacciones pendientes", e);
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
            Transaction transaction = em.find(Transaction.class, id);
            if (transaction != null) {
                em.remove(transaction);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Error al eliminar transacción", e);
            throw new RuntimeException("No se pudo eliminar la transacción", e);
        } finally {
            em.close();
        }
    }

    @Override
    public double sumByUserAndType(User user, String transactionType) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Double> query = em.createQuery(
                    "SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.transactionType = :type AND t.status = 'completed'",
                    Double.class);
            query.setParameter("user", user);
            query.setParameter("type", transactionType);
            Double result = query.getSingleResult();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al calcular suma de transacciones por usuario y tipo", e);
            return 0.0;
        } finally {
            em.close();
        }
    }
}