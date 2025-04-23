package com.ztake.casino.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Clase que representa un ticket de soporte.
 */
@Entity
@Table(name = "support_tickets")
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_updated_date", nullable = false)
    private LocalDateTime lastUpdatedDate;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // Constructor por defecto requerido por JPA
    public SupportTicket() {
        this.createdDate = LocalDateTime.now();
        this.lastUpdatedDate = this.createdDate;
        this.status = "open";
    }

    public SupportTicket(User user, String subject, String category, String message) {
        this();
        this.user = user;
        this.subject = subject;
        this.category = category;
        this.message = message;
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    /**
     * Actualiza el estado del ticket y la fecha de última actualización
     * @param newStatus nuevo estado del ticket
     * @param notes notas administrativas (opcional)
     */
    public void updateStatus(String newStatus, String notes) {
        this.status = newStatus;
        if (notes != null && !notes.isEmpty()) {
            this.adminNotes = (this.adminNotes != null ? this.adminNotes + "\n\n" : "") +
                    LocalDateTime.now() + ": " + notes;
        }
        this.lastUpdatedDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SupportTicket{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", subject='" + subject + '\'' +
                ", category='" + category + '\'' +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}