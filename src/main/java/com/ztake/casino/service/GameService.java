package com.ztake.casino.service;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz para el servicio de gestión de juegos.
 */
public interface GameService {

    /**
     * Inicia una nueva sesión de juego con una apuesta inicial.
     *
     * @param user usuario que juega
     * @param gameType tipo de juego (Mines, Slots, etc.)
     * @param betAmount cantidad apostada
     * @return la sesión de juego creada
     * @throws IllegalArgumentException si la apuesta es inválida
     * @throws IllegalStateException si el usuario no tiene saldo suficiente
     */
    GameSession startGame(User user, String gameType, double betAmount);

    /**
     * Finaliza una sesión de juego con ganancias.
     *
     * @param gameSession la sesión de juego
     * @param winnings cantidad ganada (0 si perdió)
     * @param result resultado del juego (won, lost)
     * @param gameData datos específicos del juego en formato JSON (opcional)
     * @return la sesión de juego actualizada
     */
    GameSession endGame(GameSession gameSession, double winnings, String result, String gameData);

    /**
     * Obtiene el historial de juegos de un usuario.
     *
     * @param user el usuario
     * @return lista de sesiones de juego
     */
    List<GameSession> getUserGameHistory(User user);

    /**
     * Obtiene el historial de juegos de un usuario filtrado por tipo de juego y rango de fechas.
     *
     * @param user el usuario
     * @param gameType el tipo de juego (null para todos)
     * @param fromDate fecha de inicio (null para sin límite)
     * @param toDate fecha de fin (null para sin límite)
     * @return lista de sesiones de juego filtradas
     */
    List<GameSession> getUserGameHistory(User user, String gameType, LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Calcula estadísticas de juego para un usuario.
     *
     * @param user el usuario
     * @return mapa con estadísticas (total apostado, total ganado, balance neto, partidas jugadas)
     */
    java.util.Map<String, Object> calculateUserGameStats(User user);
}