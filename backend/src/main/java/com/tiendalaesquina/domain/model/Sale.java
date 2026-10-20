package com.tiendalaesquina.domain.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_number", nullable = false, unique = true, length = 40)
    private String saleNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sold_by_id", nullable = false)
    private UserAccount soldBy;

    @Column(name = "sale_date", nullable = false)
    private Instant saleDate;

    @Column(name = "subtotal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "cash_received", nullable = false, precision = 14, scale = 2)
    private BigDecimal cashReceived;

    @Column(name = "change_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<SaleItem> items = new ArrayList<>();

    protected Sale() {
    }

    public Sale(String saleNumber,
                UserAccount soldBy,
                BigDecimal subtotalAmount,
                BigDecimal totalAmount,
                BigDecimal cashReceived,
                BigDecimal changeAmount,
                String notes) {
        this.saleNumber = saleNumber;
        this.soldBy = soldBy;
        this.subtotalAmount = subtotalAmount;
        this.totalAmount = totalAmount;
        this.cashReceived = cashReceived;
        this.changeAmount = changeAmount;
        this.notes = notes;
        this.status = SaleStatus.COMPLETED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        if (saleDate == null) {
            saleDate = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

    public void addItem(SaleItem item) {
        items.add(item);
    }

    public Long getId() {
        return id;
    }

    public String getSaleNumber() {
        return saleNumber;
    }

    public UserAccount getSoldBy() {
        return soldBy;
    }

    public Instant getSaleDate() {
        return saleDate;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getCashReceived() {
        return cashReceived;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SaleItem> getItems() {
        return items;
    }
}