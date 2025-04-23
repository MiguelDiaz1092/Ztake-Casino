package com.ztake.casino.service;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.Transaction;
import com.ztake.casino.model.User;
import com.ztake.casino.repository.GameSessionRepository;
import com.ztake.casino.repository.TransactionRepository;
import com.ztake.casino.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementación del servicio de gestión de juegos.
 */
public class GameServiceImpl implements GameService {
    private static final Logger LOGGER = Logger.getLogger(GameServiceImpl.class.getName());

    private final GameSessionRepository gameSessionRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public GameServiceImpl(GameSessionRepository gameSessionRepository,
                           TransactionRepository transactionRepository,
                           UserRepository userRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public GameSession startGame(User user, String gameType, double betAmount) {
        // Validar la apuesta
        if (betAmount <= 0) {
            throw new IllegalArgumentException("La apuesta debe ser mayor que cero");
        }

        // Validar el saldo
        if (user.getBalance() < betAmount) {
            throw new IllegalStateException("Saldo insuficiente para realizar la apuesta");
        }

        // Crear transacción de apuesta
        Transaction betTransaction = new Transaction(user, betAmount, "bet");

        // Actualizar saldo del usuario
        user.setBalance(user.getBalance() - betAmount);
        userRepository.save(user);

        // Completar transacción
        betTransaction.setStatus("completed");
        transactionRepository.save(betTransaction);

        // Crear sesión de juego
        GameSession gameSession = new GameSession();
        gameSession.setUser(user);
        gameSession.setGameType(gameType);
        gameSession.setBetAmount(betAmount);
        gameSession.setWinningAmount(0.0); // Inicialmente 0, se actualiza al finalizar
        gameSession.setResult("in_progress");
        gameSession.setSessionDate(LocalDateTime.now());

        LOGGER.info("Iniciando juego: " + gameType + " - Usuario: " + user.getUsername() + " - Apuesta: " + betAmount);
        return gameSessionRepository.save(gameSession);
    }

    @Override
    public GameSession endGame(GameSession gameSession, double winnings, String result, String gameData) {
        // Validar estado de la sesión
        if (!"in_progress".equals(gameSession.getResult())) {
            throw new IllegalStateException("La sesión de juego ya está finalizada");
        }

        // Actualizar la sesión
        gameSession.setWinningAmount(winnings);
        gameSession.setResult(result);
        gameSession.setGameData(gameData);

        // Si hay ganancias, crear transacción y actualizar saldo
        if (winnings > 0) {
            User user = gameSession.getUser();

            // Crear transacción de ganancia
            Transaction winTransaction = new Transaction(user, winnings, "win");

            // Actualizar saldo del usuario
            user.setBalance(user.getBalance() + winnings);
            userRepository.save(user);

            // Completar transacción
            winTransaction.setStatus("completed");
            transactionRepository.save(winTransaction);

            LOGGER.info("Juego finalizado con ganancias - Usuario: " + user.getUsername() +
                    " - Tipo: " + gameSession.getGameType() +
                    " - Apuesta: " + gameSession.getBetAmount() +
                    " - Ganancias: " + winnings);
        } else {
            LOGGER.info("Juego finalizado sin ganancias - Usuario: " + gameSession.getUser().getUsername() +
                    " - Tipo: " + gameSession.getGameType() +
                    " - Apuesta: " + gameSession.getBetAmount());
        }

        // Guardar y retornar la sesión actualizada
        return gameSessionRepository.save(gameSession);
    }

    @Override
    public List<GameSession> getUserGameHistory(User user) {
        return gameSessionRepository.findByUser(user);
    }

    @Override
    public List<GameSession> getUserGameHistory(User user, String gameType, LocalDateTime fromDate, LocalDateTime toDate) {
        // Aplicar filtros según los parámetros proporcionados
        if (gameType != null && fromDate != null && toDate != null) {
            return gameSessionRepository.findByUserAndGameTypeAndDateRange(user, gameType, fromDate, toDate);
        } else if (gameType != null) {
            return gameSessionRepository.findByUserAndGameType(user, gameType);
        } else if (fromDate != null && toDate != null) {
            return gameSessionRepository.findByUserAndDateRange(user, fromDate, toDate);
        } else {
            return gameSessionRepository.findByUser(user);
        }
    }

    @Override
    public Map<String, Object> calculateUserGameStats(User user) {
        Map<String, Object> stats = new HashMap<>();
        List<GameSession> gameSessions = gameSessionRepository.findByUser(user);

        double totalBet = 0.0;
        double totalWon = 0.0;
        int gamesPlayed = gameSessions.size();
        int gamesWon = 0;

        for (GameSession session : gameSessions) {
            totalBet += session.getBetAmount();
            totalWon += session.getWinningAmount();

            if ("won".equals(session.getResult())) {
                gamesWon++;
            }
        }

        double netBalance = totalWon - totalBet;
        double winRate = gamesPlayed > 0 ? (double) gamesWon / gamesPlayed * 100 : 0;

        stats.put("totalBet", totalBet);
        stats.put("totalWon", totalWon);
        stats.put("netBalance", netBalance);
        stats.put("gamesPlayed", gamesPlayed);
        stats.put("gamesWon", gamesWon);
        stats.put("winRate", winRate);

        return stats;
    }
}