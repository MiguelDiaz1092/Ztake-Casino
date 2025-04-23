package com.ztake.casino.repository;

import com.ztake.casino.model.SupportTicket;
import com.ztake.casino.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el repositorio de tickets de soporte.
 */
public interface SupportTicketRepository {

    /**
     * Guarda un nuevo ticket de soporte o actualiza uno existente.
     *
     * @param ticket el ticket a guardar
     * @return el ticket guardado
     */
    SupportTicket save(SupportTicket ticket);

    /**
     * Busca un ticket por su ID.
     *
     * @param id ID del ticket
     * @return Optional con el ticket si existe
     */
    Optional<SupportTicket> findById(Long id);

    /**
     * Obtiene todos los tickets de un usuario.
     *
     * @param user el usuario
     * @return lista de tickets
     */
    List<SupportTicket> findByUser(User user);

    /**
     * Obtiene todos los tickets con un estado específico.
     *
     * @param status el estado del ticket
     * @return lista de tickets
     */
    List<SupportTicket> findByStatus(String status);

    /**
     * Obtiene todos los tickets de un usuario con un estado específico.
     *
     * @param user el usuario
     * @param status el estado del ticket
     * @return lista de tickets
     */
    List<SupportTicket> findByUserAndStatus(User user, String status);

    /**
     * Elimina un ticket por su ID.
     *
     * @param id ID del ticket
     */
    void deleteById(Long id);
}