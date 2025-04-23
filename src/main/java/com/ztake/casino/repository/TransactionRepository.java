package com.ztake.casino.repository;

import com.ztake.casino.model.Transaction;
import com.ztake.casino.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el repositorio de transacciones.
 */
public interface TransactionRepository {

    /**
     * Guarda una nueva transacción o actualiza una existente.
     *
     * @param transaction la transacción a guardar
     * @return la transacción guardada
     */
    Transaction save(Transaction transaction);

    /**
     * Busca una transacción por su ID.
     *
     * @param id ID de la transacción
     * @return Optional con la transacción si existe
     */
    Optional<Transaction> findById(Long id);

    /**
     * Obtiene todas las transacciones de un usuario.
     *
     * @param user el usuario
     * @return lista de transacciones
     */
    List<Transaction> findByUser(User user);

    /**
     * Obtiene todas las transacciones de un usuario en un rango de fechas.
     *
     * @param user el usuario
     * @param fromDate fecha de inicio
     * @param toDate fecha de fin
     * @return lista de transacciones
     */
    List<Transaction> findByUserAndDateRange(User user, LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Obtiene todas las transacciones de un usuario para un tipo específico.
     *
     * @param user el usuario
     * @param transactionType el tipo de transacción
     * @return lista de transacciones
     */
    List<Transaction> findByUserAndType(User user, String transactionType);

    /**
     * Obtiene todas las transacciones pendientes.
     *
     * @return lista de transacciones pendientes
     */
    List<Transaction> findPendingTransactions();

    /**
     * Elimina una transacción por su ID.
     *
     * @param id ID de la transacción
     */
    void deleteById(Long id);

    /**
     * Obtiene el balance total de transacciones de un tipo específico para un usuario.
     *
     * @param user el usuario
     * @param transactionType el tipo de transacción
     * @return la suma total de las transacciones
     */
    double sumByUserAndType(User user, String transactionType);
}