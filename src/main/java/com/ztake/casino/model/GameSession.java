package com.ztake.casino.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Clase que representa una sesión de juego.
 */
@Entity
@Table(name = "game_sessions")
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_type", nullable = false, length = 50)
    private String gameType;

    @Column(name = "bet_amount", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private double betAmount;

    @Column(name = "winning_amount", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private double winningAmount;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(name = "game_data", columnDefinition = "TEXT")
    private String gameData;

    // Constructor por defecto requerido por JPA
    public GameSession() {
        this.sessionDate = LocalDateTime.now();
    }

    public GameSession(User user, String gameType, double betAmount, double winningAmount, String result) {
        this();
        this.user = user;
        this.gameType = gameType;
        this.betAmount = betAmount;
        this.winningAmount = winningAmount;
        this.result = result;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public double getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }

    public double getWinningAmount() {
        return winningAmount;
    }

    public void setWinningAmount(double winningAmount) {
        this.winningAmount = winningAmount;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getGameData() {
        return gameData;
    }

    public void setGameData(String gameData) {
        this.gameData = gameData;
    }

    @Override
    public String toString() {
        return "GameSession{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", gameType='" + gameType + '\'' +
                ", betAmount=" + betAmount +
                ", winningAmount=" + winningAmount +
                ", result='" + result + '\'' +
                ", sessionDate=" + sessionDate +
                '}';
    }
}