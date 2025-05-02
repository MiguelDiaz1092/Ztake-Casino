package com.ztake.casino.controller;

import com.ztake.casino.model.GameSession;
import com.ztake.casino.model.User;
import com.ztake.casino.service.GameService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador mejorado para el juego Mines con integración de base de datos.
 */
public class MinesGameController {
    private static final Logger LOGGER = Logger.getLogger(MinesGameController.class.getName());

    @FXML
    private Label balanceLabel;

    @FXML
    private TextField betAmountField;

    @FXML
    private Label minesCountLabel;

    @FXML
    private GridPane gameBoard;

    @FXML
    private Button cashoutButton;

    @FXML
    private Label potentialWinningsLabel;

    @FXML
    private Slider minesSlider;

    @FXML
    private Label gameInfoLabel;

    private User currentUser;
    private GameService gameService;
    private GameSession currentGameSession;
    private boolean updateInProgress = false;

    private double currentBet = 5.00;
    private int minesCount = 5;
    private double currentMultiplier = 1.00;
    private boolean gameStarted = false;
    private boolean firstCellClicked = false;
    private List<Button> cellButtons = new ArrayList<>();
    private boolean[][] mineLocations = new boolean[4][4]; // true = mina, false = gema

    // Coordenadas de la primera celda seleccionada
    private int firstClickRow = -1;
    private int firstClickCol = -1;

    // Imágenes para las celdas
    private Image gemImage;
    private Image bombImage;

    /**
     * Inicializa el controlador después de que el FXML ha sido cargado.
     */
    @FXML
    public void initialize() {
        // Inicializar valores predeterminados
        betAmountField.setText(String.format("%.2f", currentBet));
        minesCountLabel.setText(String.valueOf(minesCount));
        potentialWinningsLabel.setText(String.format("%.2f", currentBet * calculateMultiplier()));

        // Configurar el slider de minas
        if (minesSlider != null) {
            minesSlider.setMin(1);
            minesSlider.setMax(10);
            minesSlider.setValue(minesCount);
            minesSlider.setBlockIncrement(1);
            minesSlider.setMajorTickUnit(1);
            minesSlider.setMinorTickCount(0);
            minesSlider.setShowTickLabels(true);
            minesSlider.setShowTickMarks(true);
            minesSlider.setSnapToTicks(true);

            minesSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                minesCount = newVal.intValue();
                minesCountLabel.setText(String.valueOf(minesCount));
                updatePotentialWinnings();
            });
        }

        // Validación de entrada para apuesta
        betAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                betAmountField.setText(oldVal);
                return;
            }

            try {
                double bet = Double.parseDouble(newVal);
                if (bet > 0) {
                    currentBet = bet;
                    updatePotentialWinnings();
                }
            } catch (NumberFormatException e) {
                // Ignorar excepciones de conversión
            }
        });

        // Cargar imágenes con manejo de errores mejorado
        loadImages();

        // Inicializar el tablero
        initializeGameBoard();
    }

    /**
     * Carga las imágenes del juego con manejo de errores mejorado.
     */
    private void loadImages() {
        try {
            InputStream gemStream = getClass().getResourceAsStream("/images/gem.png");
            if (gemStream != null) {
                gemImage = new Image(gemStream);
                gemStream.close();
            } else {
                LOGGER.warning("No se pudo encontrar la imagen: /images/gem.png");
            }

            InputStream bombStream = getClass().getResourceAsStream("/images/bomb.png");
            if (bombStream != null) {
                bombImage = new Image(bombStream);
                bombStream.close();
            } else {
                LOGGER.warning("No se pudo encontrar la imagen: /images/bomb.png");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar imágenes: " + e.getMessage(), e);
            LOGGER.info("Se usarán emojis como alternativa a las imágenes");
        }
    }

    /**
     * Configura el usuario actual para mostrar su saldo.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateBalanceLabel();
    }

    /**
     * Configura el servicio de juego.
     */
    public void setGameService(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Inicializa el tablero de juego con botones.
     */
    private void initializeGameBoard() {
        // Limpiar el tablero y la lista de botones
        gameBoard.getChildren().clear();
        cellButtons.clear();

        // Crear un tablero 4x4
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                Button cellButton = new Button();
                cellButton.getStyleClass().add("game-cell");

                // Evento de clic para revelar celda
                final int r = row;
                final int c = col;
                cellButton.setOnAction(e -> handleCellClick(cellButton, r, c));

                // Agregar a la cuadrícula y a la lista
                gameBoard.add(cellButton, col, row);
                cellButtons.add(cellButton);

                // Inicializar posición como segura (sin mina)
                mineLocations[row][col] = false;
            }
        }
    }

    /**
     * Prepara el tablero colocando minas aleatoriamente, asegurando que la primera celda
     * seleccionada nunca tenga una mina.
     */
    private void setupMineLocations() {
        // Reiniciar tablero
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                mineLocations[i][j] = false;
            }
        }

        // Verificar que tenemos una posición válida para el primer clic
        if (firstClickRow < 0 || firstClickCol < 0) {
            LOGGER.severe("Error al configurar el tablero: coordenadas inválidas para el primer clic");
            return;
        }

        // Colocar minas aleatoriamente, asegurando que la primera casilla seleccionada no tenga mina
        Random random = new Random();
        int minesPlaced = 0;

        while (minesPlaced < minesCount) {
            int row = random.nextInt(4);
            int col = random.nextInt(4);

            // No colocar mina en la posición del primer clic
            if (!mineLocations[row][col] && (row != firstClickRow || col != firstClickCol)) {
                mineLocations[row][col] = true;
                minesPlaced++;
            }
        }

        LOGGER.info("Tablero preparado con " + minesCount + " minas, asegurando que la posición (" +
                firstClickRow + "," + firstClickCol + ") no tiene mina");
    }

    /**
     * Maneja el clic en una celda del tablero.
     */
    private void handleCellClick(Button button, int row, int col) {
        if (updateInProgress) {
            LOGGER.info("Operación en progreso, ignorando clic");
            return;
        }

        if (!gameStarted) {
            // Iniciar un juego nuevo
            startNewGame(button, row, col);
        } else {
            // Continuar un juego en progreso
            continueGame(button, row, col);
        }
    }

    /**
     * Inicia un nuevo juego cuando se hace clic en una celda.
     */
    private void startNewGame(Button button, int row, int col) {
        updateInProgress = true;

        try {
            // Guardar las coordenadas del primer clic
            firstClickRow = row;
            firstClickCol = col;

            // Iniciar juego con la primera celda
            gameStarted = true;
            firstCellClicked = true;

            // Preparar el tablero asegurando que el primer clic sea siempre en una gema
            setupMineLocations();

            // Deshabilitar slider y campo de apuesta
            if (minesSlider != null) {
                minesSlider.setDisable(true);
            }
            betAmountField.setDisable(true);

            // Iniciar sesión de juego en la base de datos
            if (gameService != null && currentUser != null) {
                // Verificar saldo suficiente
                if (currentUser.getBalance() < currentBet) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Saldo insuficiente para realizar esta apuesta.");
                    resetGame();
                    updateInProgress = false;
                    return;
                }

                try {
                    // Crear sesión de juego y actualizar saldo
                    currentGameSession = gameService.startGame(currentUser, "Mines", currentBet);

                    // Actualizar la UI
                    updateBalanceLabel();

                    if (gameInfoLabel != null) {
                        gameInfoLabel.setText("¡Juego en curso! Encuentra las gemas y evita las minas.");
                    }

                    // Procesar el primer clic ahora que el juego está configurado
                    showGem(button);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al iniciar el juego: " + e.getMessage(), e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error al iniciar el juego: " + e.getMessage());
                    resetGame();
                    updateInProgress = false;
                    return;
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar juego: " + e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo iniciar el juego: " + e.getMessage());
            resetGame();
        } finally {
            updateInProgress = false;
        }
    }

    /**
     * Continúa un juego en progreso cuando se hace clic en una celda.
     */
    private void continueGame(Button button, int row, int col) {
        updateInProgress = true;

        try {
            // Verificar si la celda tiene una mina
            boolean isGem = !mineLocations[row][col];

            if (isGem) {
                // Es una gema
                showGem(button);
            } else {
                // Es una bomba
                showBomb(button);

                // Juego perdido
                endGame(false);
            }
        } finally {
            updateInProgress = false;
        }
    }

    /**
     * Muestra una gema en el botón y actualiza el multiplicador.
     */
    private void showGem(Button button) {
        if (gemImage != null) {
            ImageView gemView = new ImageView(gemImage);
            gemView.setFitWidth(60);
            gemView.setFitHeight(60);
            gemView.setPreserveRatio(true);
            button.setGraphic(gemView);
        } else {
            // Si no hay imagen, usar texto
            button.setText("💎");
        }

        // Deshabilitar el botón para que no se pueda volver a hacer clic
        button.setDisable(true);

        // Aumentar multiplicador
        currentMultiplier = calculateMultiplier();
        updatePotentialWinnings();
    }

    /**
     * Muestra una bomba en el botón.
     */
    private void showBomb(Button button) {
        if (bombImage != null) {
            ImageView bombView = new ImageView(bombImage);
            bombView.setFitWidth(60);
            bombView.setFitHeight(60);
            bombView.setPreserveRatio(true);
            button.setGraphic(bombView);
        } else {
            // Si no hay imagen, usar texto
            button.setText("💣");
        }

        // Deshabilitar el botón para que no se pueda volver a hacer clic
        button.setDisable(true);
    }

    /**
     * Actualiza la etiqueta de ganancias potenciales.
     */
    private void updatePotentialWinnings() {
        if (potentialWinningsLabel != null) {
            double winnings = currentBet * currentMultiplier;
            // Redondear a 2 decimales para evitar errores de precisión
            BigDecimal bd = new BigDecimal(winnings).setScale(2, RoundingMode.HALF_UP);
            potentialWinningsLabel.setText(String.format("%.2f", bd.doubleValue()));
        }
    }

    /**
     * Calcula el multiplicador según el número de minas y celdas descubiertas.
     */
    private double calculateMultiplier() {
        // Calcular cuántas celdas han sido reveladas
        int revealedCells = countRevealedCells();

        // Fórmula base para el multiplicador: mayor con más minas y más celdas descubiertas
        double baseMult = 1.0 + (minesCount * 0.05) + (revealedCells * 0.1);

        // Redondear a 2 decimales para evitar errores de precisión
        BigDecimal bd = new BigDecimal(baseMult).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Maneja el evento de clic en el botón SALIR (cashout).
     */
    @FXML
    public void handleCashoutButtonAction(ActionEvent event) {
        if (updateInProgress) {
            LOGGER.info("Operación en progreso, ignorando clic en SALIR");
            return;
        }

        updateInProgress = true;

        try {
            if (gameStarted && firstCellClicked) {
                // Finalizar el juego con victoria
                endGame(true);
            } else {
                // Si no ha comenzado, volver al dashboard
                navigateToDashboard();
            }
        } finally {
            updateInProgress = false;
        }
    }

    /**
     * Finaliza el juego actual.
     * @param isWin true si el jugador ganó, false si perdió
     */
    private void endGame(boolean isWin) {
        if (updateInProgress) {
            return;
        }

        updateInProgress = true;

        try {
            // Deshabilitar todas las celdas
            for (Button button : cellButtons) {
                button.setDisable(true);
            }

            // Revelar todas las minas si perdió
            if (!isWin) {
                revealAllMines();
            }

            if (gameService != null && currentGameSession != null) {
                try {
                    double winnings = 0;
                    if (isWin) {
                        // Calcular ganancias
                        winnings = currentBet * currentMultiplier;

                        // Redondear a 2 decimales para evitar errores de precisión
                        BigDecimal bd = new BigDecimal(winnings).setScale(2, RoundingMode.HALF_UP);
                        winnings = bd.doubleValue();
                    }

                    // Guardar datos del juego para análisis
                    JSONObject gameData = new JSONObject();
                    gameData.put("minesCount", minesCount);
                    gameData.put("multiplier", currentMultiplier);
                    gameData.put("revealedCells", countRevealedCells());

                    // Finalizar la sesión en la base de datos
                    gameService.endGame(
                            currentGameSession,
                            winnings,
                            isWin ? "won" : "lost",
                            gameData.toString()
                    );

                    // Actualizar el usuario y la UI
                    if (currentUser != null) {
                        updateBalanceLabel();
                    }

                    // Mostrar mensaje según resultado
                    if (isWin) {
                        showAlert(Alert.AlertType.INFORMATION, "¡Victoria!",
                                "Has ganado " + String.format("%.2f", winnings));

                        if (gameInfoLabel != null) {
                            gameInfoLabel.setText("¡Has ganado! Puedes comenzar un nuevo juego.");
                        }
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "¡Has perdido!",
                                "Has encontrado una mina y perdido tu apuesta de " + String.format("%.2f", currentBet));

                        if (gameInfoLabel != null) {
                            gameInfoLabel.setText("¡Has perdido! Puedes comenzar un nuevo juego.");
                        }
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al finalizar el juego: " + e.getMessage(), e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Ocurrió un error al finalizar el juego: " + e.getMessage());
                }
            }

            // Reiniciar juego
            resetGame();
        } finally {
            updateInProgress = false;
        }
    }

    /**
     * Restablece el estado del juego.
     */
    private void resetGame() {
        gameStarted = false;
        firstCellClicked = false;
        firstClickRow = -1;
        firstClickCol = -1;
        currentMultiplier = 1.00;

        if (minesSlider != null) {
            minesSlider.setDisable(false);
        }
        betAmountField.setDisable(false);
        initializeGameBoard();
        currentGameSession = null;
        updatePotentialWinnings();
    }

    /**
     * Revela todas las minas en el tablero.
     */
    private void revealAllMines() {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (mineLocations[row][col]) {
                    Button button = cellButtons.get(row * 4 + col);
                    if (!button.isDisabled()) {  // Solo si aún no se ha revelado
                        showBomb(button);
                    }
                }
            }
        }
    }

    /**
     * Cuenta cuántas celdas se han revelado.
     */
    private int countRevealedCells() {
        int count = 0;
        for (Button button : cellButtons) {
            if (button.isDisabled()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Actualiza la etiqueta de saldo con el valor actual.
     * Obtiene el saldo más reciente del usuario de la base de datos.
     */
    private void updateBalanceLabel() {
        if (currentUser != null && balanceLabel != null) {
            try {
                // Aquí podríamos consultar el saldo actualizado de la base de datos
                // pero para simplicidad usamos el valor que ya tenemos en memoria
                balanceLabel.setText(String.format("%.2f", currentUser.getBalance()));
                LOGGER.info("Saldo actualizado: " + currentUser.getBalance());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al actualizar etiqueta de saldo", e);
            }
        }
    }

    /**
     * Navega de vuelta al dashboard principal.
     */
    private void navigateToDashboard() {
        try {
            // Cargar la vista principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            Parent mainRoot = loader.load();

            // Obtener el controlador
            MainController mainController = loader.getController();

            // Configurar el controlador con los datos del usuario
            if (currentUser != null) {
                mainController.setCurrentUser(currentUser);
            }

            // Configurar servicios
            if (gameService != null) {
                mainController.setGameService(gameService);
            }

            // Obtener la ventana actual
            Stage stage = (Stage) cashoutButton.getScene().getWindow();

            // Crear una nueva escena con la vista principal
            if (stage != null && stage.getScene() != null) {
                stage.getScene().setRoot(mainRoot);
            } else {
                LOGGER.severe("Error: No se pudo acceder a la ventana o escena actual");
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar la vista principal: " + e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la vista principal: " + e.getMessage());
        }
    }

    /**
     * Muestra una alerta con el mensaje especificado.
     */
    private void showAlert(Alert.AlertType type, String header, String content) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(type);
                alert.setTitle("Mines");
                alert.setHeaderText(header);
                alert.setContentText(content);
                alert.showAndWait();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al mostrar alerta: " + e.getMessage(), e);
            }
        });
    }
}