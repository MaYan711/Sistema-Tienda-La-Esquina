package com.tiendalaesquina.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "employee_operating_hours")
public class EmployeeOperatingHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, unique = true, length = 16)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private UserAccount updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmployeeOperatingHour() {
    }

    public EmployeeOperatingHour(
            DayOfWeek dayOfWeek,
            boolean enabled,
            LocalTime openingTime,
            LocalTime closingTime,
            UserAccount updatedBy
    ) {
        this.dayOfWeek = dayOfWeek;
        this.enabled = enabled;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
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

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public UserAccount getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            boolean enabled,
            LocalTime openingTime,
            LocalTime closingTime,
            UserAccount actor
    ) {
        this.enabled = enabled;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.updatedBy = actor;
    }
}
