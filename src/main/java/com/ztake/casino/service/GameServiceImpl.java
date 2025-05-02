package com.ztake.casino.service;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.Transaction;
import com.ztake.casino.model.User;
import com.ztake.casino.repository.GameSessionRepository;
import com.ztake.casino.repository.TransactionRepository;
import com.ztake.casino.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación mejorada del servicio de gestión de juegos.
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
    public synchronized GameSession startGame(User user, String gameType, double betAmount) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        if (gameType == null || gameType.trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de juego no puede estar vacío");
        }

        // Validar la apuesta con mayor precisión
        validateBetAmount(betAmount);

        // Crear una copia del usuario para evitar problemas de concurrencia
        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar el saldo con mayor precisión
        BigDecimal userBalance = BigDecimal.valueOf(updatedUser.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bet = BigDecimal.valueOf(betAmount).setScale(2, RoundingMode.HALF_UP);

        if (userBalance.compareTo(bet) < 0) {
            throw new IllegalStateException("Saldo insuficiente para realizar la apuesta");
        }

        try {
            // Crear transacción de apuesta
            Transaction betTransaction = new Transaction(updatedUser, bet.doubleValue(), "bet");

            // Actualizar saldo del usuario
            BigDecimal newBalance = userBalance.subtract(bet);
            updatedUser.setBalance(newBalance.doubleValue());
            userRepository.save(updatedUser);

            // Actualizar la referencia al usuario
            user.setBalance(updatedUser.getBalance());

            // Completar transacción
            betTransaction.setStatus("completed");
            transactionRepository.save(betTransaction);

            // Crear sesión de juego
            GameSession gameSession = new GameSession();
            gameSession.setUser(updatedUser);
            gameSession.setGameType(gameType);
            gameSession.setBetAmount(bet.doubleValue());
            gameSession.setWinningAmount(0.0); // Inicialmente 0, se actualiza al finalizar
            gameSession.setResult("in_progress");
            gameSession.setSessionDate(LocalDateTime.now());

            LOGGER.info("Iniciando juego: " + gameType + " - Usuario: " + updatedUser.getUsername() + " - Apuesta: " + bet);
            return gameSessionRepository.save(gameSession);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar el juego: " + e.getMessage(), e);
            throw new RuntimeException("Error al iniciar el juego: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized GameSession endGame(GameSession gameSession, double winnings, String result, String gameData) {
        if (gameSession == null) {
            throw new IllegalArgumentException("La sesión de juego no puede ser nula");
        }

        // Validar estado de la sesión
        if (!"in_progress".equals(gameSession.getResult())) {
            throw new IllegalStateException("La sesión de juego ya está finalizada");
        }

        // Validar resultado
        if (result == null || (!result.equals("won") && !result.equals("lost"))) {
            throw new IllegalArgumentException("Resultado inválido: debe ser 'won' o 'lost'");
        }

        // Redondear ganancias a 2 decimales para evitar errores de precisión
        BigDecimal winningsBD = BigDecimal.valueOf(winnings).setScale(2, RoundingMode.HALF_UP);

        try {
            // Actualizar la sesión
            gameSession.setWinningAmount(winningsBD.doubleValue());
            gameSession.setResult(result);
            gameSession.setGameData(gameData);

            // Si hay ganancias y el resultado es "won", crear transacción y actualizar saldo
            if (winningsBD.compareTo(BigDecimal.ZERO) > 0 && "won".equals(result)) {
                User user = gameSession.getUser();

                // Buscar usuario fresco de la base de datos para evitar problemas de concurrencia
                User freshUser = userRepository.findById(user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                // Crear transacción de ganancia
                Transaction winTransaction = new Transaction(freshUser, winningsBD.doubleValue(), "win");

                // Actualizar saldo del usuario
                BigDecimal currentBalance = BigDecimal.valueOf(freshUser.getBalance()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal newBalance = currentBalance.add(winningsBD);
                freshUser.setBalance(newBalance.doubleValue());
                userRepository.save(freshUser);

                // Actualizar referencia del usuario
                user.setBalance(freshUser.getBalance());

                // Completar transacción
                winTransaction.setStatus("completed");
                transactionRepository.save(winTransaction);

                LOGGER.info("Juego finalizado con ganancias - Usuario: " + user.getUsername() +
                        " - Tipo: " + gameSession.getGameType() +
                        " - Apuesta: " + gameSession.getBetAmount() +
                        " - Ganancias: " + winningsBD);
            } else {
                LOGGER.info("Juego finalizado sin ganancias - Usuario: " + gameSession.getUser().getUsername() +
                        " - Tipo: " + gameSession.getGameType() +
                        " - Apuesta: " + gameSession.getBetAmount());
            }

            // Guardar y retornar la sesión actualizada
            return gameSessionRepository.save(gameSession);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al finalizar el juego: " + e.getMessage(), e);
            throw new RuntimeException("Error al finalizar el juego: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GameSession> getUserGameHistory(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        try {
            return gameSessionRepository.findByUser(user);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener historial de juegos: " + e.getMessage(), e);
            return List.of(); // Retornar lista vacía en caso de error
        }
    }

    @Override
    public List<GameSession> getUserGameHistory(User user, String gameType, LocalDateTime fromDate, LocalDateTime toDate) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        try {
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener historial de juegos filtrado: " + e.getMessage(), e);
            return List.of(); // Retornar lista vacía en caso de error
        }
    }

    @Override
    public Map<String, Object> calculateUserGameStats(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        Map<String, Object> stats = new HashMap<>();

        try {
            List<GameSession> gameSessions = gameSessionRepository.findByUser(user);

            BigDecimal totalBet = BigDecimal.ZERO;
            BigDecimal totalWon = BigDecimal.ZERO;
            int gamesPlayed = gameSessions.size();
            int gamesWon = 0;

            for (GameSession session : gameSessions) {
                totalBet = totalBet.add(BigDecimal.valueOf(session.getBetAmount()));
                totalWon = totalWon.add(BigDecimal.valueOf(session.getWinningAmount()));

                if ("won".equals(session.getResult())) {
                    gamesWon++;
                }
            }

            // Redondear a 2 decimales
            totalBet = totalBet.setScale(2, RoundingMode.HALF_UP);
            totalWon = totalWon.setScale(2, RoundingMode.HALF_UP);

            BigDecimal netBalance = totalWon.subtract(totalBet);
            double winRate = gamesPlayed > 0 ? (double) gamesWon / gamesPlayed * 100 : 0;

            stats.put("totalBet", totalBet.doubleValue());
            stats.put("totalWon", totalWon.doubleValue());
            stats.put("netBalance", netBalance.doubleValue());
            stats.put("gamesPlayed", gamesPlayed);
            stats.put("gamesWon", gamesWon);
            stats.put("winRate", winRate);

            return stats;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al calcular estadísticas de juego: " + e.getMessage(), e);

            // Devolver estadísticas vacías en caso de error
            stats.put("totalBet", 0.0);
            stats.put("totalWon", 0.0);
            stats.put("netBalance", 0.0);
            stats.put("gamesPlayed", 0);
            stats.put("gamesWon", 0);
            stats.put("winRate", 0.0);

            return stats;
        }
    }

    /**
     * Valida que la cantidad apostada sea válida.
     *
     * @param betAmount la cantidad apostada
     * @throws IllegalArgumentException si la apuesta es inválida
     */
    private void validateBetAmount(double betAmount) {
        if (betAmount <= 0) {
            throw new IllegalArgumentException("La apuesta debe ser mayor que cero");
        }

        // Limitar la precisión a 2 decimales
        BigDecimal bd = BigDecimal.valueOf(betAmount);
        BigDecimal roundedBd = bd.setScale(2, RoundingMode.HALF_UP);

        if (bd.compareTo(roundedBd) != 0) {
            throw new IllegalArgumentException("La apuesta no puede tener más de 2 decimales");
        }

        // Validar límites de apuesta (se pueden ajustar según necesidades)
        if (betAmount < 0.01) {
            throw new IllegalArgumentException("La apuesta mínima es 0.01");
        }

        if (betAmount > 10000) {
            throw new IllegalArgumentException("La apuesta máxima es 10000");
        }
    }
}