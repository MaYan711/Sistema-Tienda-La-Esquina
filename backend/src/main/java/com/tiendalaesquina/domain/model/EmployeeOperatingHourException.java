package com.tiendalaesquina.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "employee_operating_hour_exceptions")
public class EmployeeOperatingHourException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exception_date", nullable = false, unique = true)
    private LocalDate exceptionDate;

    @Column(nullable = false)
    private boolean closed;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(length = 160)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private UserAccount updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmployeeOperatingHourException() {
    }

    public EmployeeOperatingHourException(
            LocalDate exceptionDate,
            boolean closed,
            LocalTime openingTime,
            LocalTime closingTime,
            String reason,
            UserAccount updatedBy
    ) {
        this.exceptionDate = exceptionDate;
        this.closed = closed;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.reason = reason;
        this.updatedBy = updatedBy;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getExceptionDate() {
        return exceptionDate;
    }

    public boolean isClosed() {
        return closed;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public String getReason() {
        return reason;
    }

    public UserAccount getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            LocalDate exceptionDate,
            boolean closed,
            LocalTime openingTime,
            LocalTime closingTime,
            String reason,
            UserAccount actor
    ) {
        this.exceptionDate = exceptionDate;
        this.closed = closed;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.reason = reason;
        this.updatedBy = actor;
    }
}
