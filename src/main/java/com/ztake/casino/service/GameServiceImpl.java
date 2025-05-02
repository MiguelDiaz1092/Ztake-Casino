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

    // Use constants for common strings like transaction types and statuses
    private static final String TRANSACTION_TYPE_BET = "bet";
    private static final String TRANSACTION_TYPE_WIN = "win";
    private static final String TRANSACTION_TYPE_DEPOSIT = "deposit";
    private static final String TRANSACTION_STATUS_COMPLETED = "completed";
    private static final String GAME_RESULT_IN_PROGRESS = "in_progress";
    private static final String GAME_RESULT_WON = "won";
    private static final String GAME_RESULT_LOST = "lost";
    private static final int DEFAULT_SCALE = 2; // For BigDecimal operations
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

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
        validateNotNull(user, "El usuario no puede ser nulo");
        validateNotBlank(gameType, "El tipo de juego no puede estar vacío");
        validateBetAmount(betAmount);

        BigDecimal bet = BigDecimal.valueOf(betAmount).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);

        // Fetch fresh user data to ensure consistency
        User freshUser = findUserByIdOrThrow(user.getId());
        BigDecimal userBalance = BigDecimal.valueOf(freshUser.getBalance()).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);

        if (userBalance.compareTo(bet) < 0) {
            throw new IllegalStateException("Saldo insuficiente para realizar la apuesta");
        }

        try {
            // Create bet transaction (initially pending potential rollback)
            Transaction betTransaction = new Transaction(freshUser, bet.doubleValue(), TRANSACTION_TYPE_BET);
            // Note: Status is set later after successful user update

            // Update user balance (debit)
            BigDecimal newBalance = userBalance.subtract(bet);
            freshUser.setBalance(newBalance.doubleValue());
            userRepository.save(freshUser);

            // Update the original user object reference passed to the method
            user.setBalance(freshUser.getBalance());

            // Mark transaction as completed and save it
            betTransaction.setStatus(TRANSACTION_STATUS_COMPLETED);
            transactionRepository.save(betTransaction);

            // Create game session
            GameSession gameSession = new GameSession();
            gameSession.setUser(freshUser);
            gameSession.setGameType(gameType);
            gameSession.setBetAmount(bet.doubleValue());
            gameSession.setWinningAmount(0.0); // Initial winning amount is zero
            gameSession.setResult(GAME_RESULT_IN_PROGRESS);
            gameSession.setSessionDate(LocalDateTime.now());

            LOGGER.log(Level.INFO, "Iniciando juego: {0} - Usuario: {1} - Apuesta: {2}",
                    new Object[]{gameType, freshUser.getUsername(), bet});
            return gameSessionRepository.save(gameSession);

        } catch (Exception e) {
            // Consider more specific exception handling or potential rollback logic if needed
            LOGGER.log(Level.SEVERE, "Error al iniciar el juego para el usuario " + user.getUsername() + ": " + e.getMessage(), e);
            // Re-throwing as a runtime exception, consistent with original logic
            throw new RuntimeException("Error al iniciar el juego: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized GameSession endGame(GameSession gameSession, double winnings, String result, String gameData) {
        validateNotNull(gameSession, "La sesión de juego no puede ser nula");

        if (!GAME_RESULT_IN_PROGRESS.equals(gameSession.getResult())) {
            throw new IllegalStateException("La sesión de juego ya está finalizada");
        }

        validateGameResult(result);

        BigDecimal winningsBD = BigDecimal.valueOf(winnings).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);

        try {
            // Update session details
            gameSession.setWinningAmount(winningsBD.doubleValue());
            gameSession.setResult(result);
            gameSession.setGameData(gameData); // Store additional game data if provided

            // Process winnings if the game was won and winnings are positive
            if (GAME_RESULT_WON.equals(result) && winningsBD.compareTo(BigDecimal.ZERO) > 0) {
                User user = gameSession.getUser();
                // Apply the credit transaction using the extracted helper method
                User updatedUser = applyCreditTransaction(user, winningsBD, TRANSACTION_TYPE_WIN);

                // Update gameSession's user reference if necessary (depends on JPA/Hibernate caching/management)
                // gameSession.setUser(updatedUser); // Usually not needed if the user object is managed

                LOGGER.log(Level.INFO, "Juego finalizado con ganancias - Usuario: {0} - Tipo: {1} - Apuesta: {2} - Ganancias: {3}",
                        new Object[]{updatedUser.getUsername(), gameSession.getGameType(), gameSession.getBetAmount(), winningsBD});
            } else {
                LOGGER.log(Level.INFO, "Juego finalizado sin ganancias - Usuario: {0} - Tipo: {1} - Apuesta: {2}",
                        new Object[]{gameSession.getUser().getUsername(), gameSession.getGameType(), gameSession.getBetAmount()});
            }

            // Save and return the updated session
            return gameSessionRepository.save(gameSession);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al finalizar el juego ID " + gameSession.getId() + ": " + e.getMessage(), e);
            throw new RuntimeException("Error al finalizar el juego: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized User depositFunds(User user, double amount) {
        validateNotNull(user, "El usuario no puede ser nulo");
        validatePositiveAmount(amount, "El monto del depósito debe ser mayor que cero");

        BigDecimal depositAmount = BigDecimal.valueOf(amount).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
        // Ensure no precision was lost during scaling (e.g., amount wasn't 10.123)
        if (depositAmount.compareTo(BigDecimal.valueOf(amount)) != 0) {
            throw new IllegalArgumentException("El monto del depósito no puede tener más de " + DEFAULT_SCALE + " decimales");
        }

        try {
            // Apply the credit transaction using the extracted helper method
            User updatedUser = applyCreditTransaction(user, depositAmount, TRANSACTION_TYPE_DEPOSIT);

            LOGGER.log(Level.INFO, "Depósito realizado - Usuario: {0} - Monto: {1} - Nuevo saldo: {2}",
                    new Object[]{updatedUser.getUsername(), depositAmount, updatedUser.getBalance()});

            return updatedUser;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al realizar depósito para el usuario " + user.getUsername() + ": " + e.getMessage(), e);
            throw new RuntimeException("Error al realizar el depósito: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to apply a credit transaction (deposit, win) to a user's balance.
     * This method fetches the fresh user state, updates the balance, saves the user,
     * creates and saves the completed transaction.
     * It assumes it's called within a synchronized context if necessary.
     *
     * @param user            The user object (can be stale, used to get ID).
     * @param amount          The positive amount to credit.
     * @param transactionType The type of transaction (e.g., "deposit", "win").
     * @return The updated User object with the new balance.
     * @throws IllegalArgumentException if user not found or amount is invalid.
     * @throws RuntimeException         if a database error occurs.
     */
    private User applyCreditTransaction(User user, BigDecimal amount, String transactionType) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            // Internal check, should be validated before calling
            throw new IllegalArgumentException("El monto para acreditar debe ser positivo.");
        }

        // Fetch fresh user data
        User freshUser = findUserByIdOrThrow(user.getId());

        // Create transaction (status set later)
        Transaction transaction = new Transaction(freshUser, amount.doubleValue(), transactionType);

        // Update balance (credit)
        BigDecimal currentBalance = BigDecimal.valueOf(freshUser.getBalance()).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
        BigDecimal newBalance = currentBalance.add(amount);
        freshUser.setBalance(newBalance.doubleValue());

        // Save user FIRST to ensure balance is updated before transaction is marked complete
        userRepository.save(freshUser);

        // Update the original user object reference passed to the method
        user.setBalance(freshUser.getBalance());

        // Mark transaction as completed and save it
        transaction.setStatus(TRANSACTION_STATUS_COMPLETED);
        transactionRepository.save(transaction);

        return freshUser; // Return the most up-to-date user object
    }


    // --- History and Stats Methods (Unchanged from original, but reviewed) ---

    @Override
    public List<GameSession> getUserGameHistory(User user) {
        validateNotNull(user, "El usuario no puede ser nulo");
        try {
            return gameSessionRepository.findByUser(user);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener historial de juegos para usuario " + user.getUsername() + ": " + e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }

    @Override
    public List<GameSession> getUserGameHistory(User user, String gameType, LocalDateTime fromDate, LocalDateTime toDate) {
        validateNotNull(user, "El usuario no puede ser nulo");
        // Basic validation for date range logic
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }

        try {
            // Delegate to repository methods based on provided filters
            if (gameType != null && fromDate != null && toDate != null) {
                return gameSessionRepository.findByUserAndGameTypeAndDateRange(user, gameType, fromDate, toDate);
            } else if (gameType != null) {
                return gameSessionRepository.findByUserAndGameType(user, gameType);
            } else if (fromDate != null && toDate != null) {
                return gameSessionRepository.findByUserAndDateRange(user, fromDate, toDate);
            } else {
                // If only user is provided, call the simpler method
                return gameSessionRepository.findByUser(user);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener historial de juegos filtrado para usuario " + user.getUsername() + ": " + e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }

    @Override
    public Map<String, Object> calculateUserGameStats(User user) {
        validateNotNull(user, "El usuario no puede ser nulo");

        Map<String, Object> stats = new HashMap<>();
        // Initialize with defaults
        stats.put("totalBet", 0.0);
        stats.put("totalWon", 0.0);
        stats.put("netBalance", 0.0);
        stats.put("gamesPlayed", 0);
        stats.put("gamesWon", 0);
        stats.put("winRate", 0.0);

        try {
            List<GameSession> gameSessions = gameSessionRepository.findByUser(user);

            if (gameSessions.isEmpty()) {
                return stats; // No games, return default stats
            }

            BigDecimal totalBet = BigDecimal.ZERO;
            BigDecimal totalWon = BigDecimal.ZERO;
            int gamesWonCount = 0;

            for (GameSession session : gameSessions) {
                // Use BigDecimal for accurate calculation
                totalBet = totalBet.add(BigDecimal.valueOf(session.getBetAmount()));
                totalWon = totalWon.add(BigDecimal.valueOf(session.getWinningAmount()));

                if (GAME_RESULT_WON.equals(session.getResult())) {
                    gamesWonCount++;
                }
            }

            // Scale results for presentation
            totalBet = totalBet.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
            totalWon = totalWon.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
            BigDecimal netBalance = totalWon.subtract(totalBet);

            int gamesPlayed = gameSessions.size();
            // Calculate win rate carefully to avoid division by zero
            double winRate = (gamesPlayed > 0)
                    ? BigDecimal.valueOf(gamesWonCount * 100.0)
                    .divide(BigDecimal.valueOf(gamesPlayed), DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
                    .doubleValue()
                    : 0.0;

            stats.put("totalBet", totalBet.doubleValue());
            stats.put("totalWon", totalWon.doubleValue());
            stats.put("netBalance", netBalance.doubleValue());
            stats.put("gamesPlayed", gamesPlayed);
            stats.put("gamesWon", gamesWonCount);
            stats.put("winRate", winRate); // Percentage

            return stats;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al calcular estadísticas de juego para usuario " + user.getUsername() + ": " + e.getMessage(), e);
            // Return default/empty stats in case of error
            return stats;
        }
    }

    // --- Validation Helper Methods ---

    /**
     * Validates that the bet amount is positive, within limits, and has acceptable precision.
     */
    private void validateBetAmount(double betAmount) {
        validatePositiveAmount(betAmount, "La apuesta debe ser mayor que cero");

        // Check precision
        BigDecimal bd = BigDecimal.valueOf(betAmount);
        BigDecimal roundedBd = bd.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
        if (bd.compareTo(roundedBd) != 0) {
            throw new IllegalArgumentException("La apuesta no puede tener más de " + DEFAULT_SCALE + " decimales");
        }

        // Check limits (adjust values as needed)
        if (betAmount < 0.01) {
            throw new IllegalArgumentException("La apuesta mínima es 0.01");
        }
        if (betAmount > 10000.00) { // Example max bet
            throw new IllegalArgumentException("La apuesta máxima es 10000.00");
        }
    }

    /**
     * Validates that a double amount is strictly positive.
     */
    private void validatePositiveAmount(double amount, String message) {
        if (amount <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that the provided game result string is either "won" or "lost".
     */
    private void validateGameResult(String result) {
        if (result == null || (!GAME_RESULT_WON.equals(result) && !GAME_RESULT_LOST.equals(result))) {
            throw new IllegalArgumentException("Resultado inválido: debe ser '" + GAME_RESULT_WON + "' o '" + GAME_RESULT_LOST + "'");
        }
    }

    /**
     * Validates that an object is not null.
     */
    private void validateNotNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that a string is not null or blank (empty or whitespace).
     */
    private void validateNotBlank(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Fetches a user by ID, throwing an IllegalArgumentException if not found.
     */
    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));
    }
}