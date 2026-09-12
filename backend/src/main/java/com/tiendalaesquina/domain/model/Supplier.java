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

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 32)
    private String nit;

    @Column(length = 32)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private UserAccount updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Supplier() {
    }

    public Supplier(String name,
                    String nit,
                    String phone,
                    String email,
                    String address,
                    UserAccount actor) {
        this.name = name;
        this.nit = nit;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = true;
        this.createdBy = actor;
        this.updatedBy = actor;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNit() {
        return nit;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    public UserAccount getCreatedBy() {
        return createdBy;
    }

    public UserAccount getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name,
                       String nit,
                       String phone,
                       String email,
                       String address,
                       UserAccount actor) {
        this.name = name;
        this.nit = nit;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.updatedBy = actor;
    }

    public void setActive(boolean active, UserAccount actor) {
        this.active = active;
        this.updatedBy = actor;
    }
}