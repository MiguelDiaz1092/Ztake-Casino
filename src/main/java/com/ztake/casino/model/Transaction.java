package com.ztake.casino.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Clase que representa una transacción financiera.
 */
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private double amount;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "reference_id", length = 50)
    private String referenceId;

    // Constructor por defecto requerido por JPA
    public Transaction() {
        this.transactionDate = LocalDateTime.now();
        this.status = "pending";
    }

    public Transaction(User user, double amount, String transactionType) {
        this();
        this.user = user;
        this.amount = amount;
        this.transactionType = transactionType;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * Método para completar la transacción y actualizar el saldo del usuario
     */
    public void complete() {
        if ("completed".equals(this.status)) {
            return; // Ya está completada
        }

        // Actualizar el saldo del usuario según el tipo de transacción
        if (user != null) {
            double currentBalance = user.getBalance();

            switch (transactionType) {
                case "deposit":
                    user.setBalance(currentBalance + amount);
                    break;
                case "withdrawal":
                    if (currentBalance >= amount) {
                        user.setBalance(currentBalance - amount);
                    } else {
                        throw new IllegalStateException("Saldo insuficiente para retirar: " + amount);
                    }
                    break;
                case "bet":
                    if (currentBalance >= amount) {
                        user.setBalance(currentBalance - amount);
                    } else {
                        throw new IllegalStateException("Saldo insuficiente para apostar: " + amount);
                    }
                    break;
                case "win":
                    user.setBalance(currentBalance + amount);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de transacción no válido: " + transactionType);
            }
        }

        // Actualizar el estado de la transacción
        this.status = "completed";
    }

    /**
     * Método para marcar la transacción como fallida
     */
    public void fail(String reason) {
        this.status = "failed";
        this.referenceId = reason;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", amount=" + amount +
                ", transactionType='" + transactionType + '\'' +
                ", status='" + status + '\'' +
                ", transactionDate=" + transactionDate +
                '}';
    }
}