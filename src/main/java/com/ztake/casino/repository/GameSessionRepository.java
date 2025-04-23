package com.ztake.casino.repository;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el repositorio de sesiones de juego.
 */
public interface GameSessionRepository {

    /**
     * Guarda una nueva sesión de juego o actualiza una existente.
     *
     * @param gameSession la sesión de juego a guardar
     * @return la sesión de juego guardada
     */
    GameSession save(GameSession gameSession);

    /**
     * Busca una sesión de juego por su ID.
     *
     * @param id ID de la sesión de juego
     * @return Optional con la sesión de juego si existe
     */
    Optional<GameSession> findById(Long id);

    /**
     * Obtiene todas las sesiones de juego de un usuario.
     *
     * @param user el usuario
     * @return lista de sesiones de juego
     */
    List<GameSession> findByUser(User user);

    /**
     * Obtiene todas las sesiones de juego de un usuario en un rango de fechas.
     *
     * @param user el usuario
     * @param fromDate fecha de inicio
     * @param toDate fecha de fin
     * @return lista de sesiones de juego
     */
    List<GameSession> findByUserAndDateRange(User user, LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Obtiene todas las sesiones de juego de un usuario para un tipo de juego específico.
     *
     * @param user el usuario
     * @param gameType el tipo de juego
     * @return lista de sesiones de juego
     */
    List<GameSession> findByUserAndGameType(User user, String gameType);

    /**
     * Obtiene todas las sesiones de juego de un usuario para un tipo de juego específico en un rango de fechas.
     *
     * @param user el usuario
     * @param gameType el tipo de juego
     * @param fromDate fecha de inicio
     * @param toDate fecha de fin
     * @return lista de sesiones de juego
     */
    List<GameSession> findByUserAndGameTypeAndDateRange(User user, String gameType, LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Elimina una sesión de juego por su ID.
     *
     * @param id ID de la sesión de juego
     */
    void deleteById(Long id);
}