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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adjusted_by_id", nullable = false)
    private UserAccount adjustedBy;

    @Column(name = "adjustment_date", nullable = false)
    private Instant adjustmentDate;

    @Column(name = "quantity_before", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityAfter;

    @Column(name = "quantity_delta", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityDelta;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryAdjustment() {
    }

    public InventoryAdjustment(Product product, UserAccount adjustedBy, Instant adjustmentDate,
                               BigDecimal quantityBefore, BigDecimal quantityAfter,
                               BigDecimal quantityDelta, String reason) {
        this.product = product;
        this.adjustedBy = adjustedBy;
        this.adjustmentDate = adjustmentDate;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.quantityDelta = quantityDelta;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (adjustmentDate == null) {
            adjustmentDate = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public UserAccount getAdjustedBy() {
        return adjustedBy;
    }

    public Instant getAdjustmentDate() {
        return adjustmentDate;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
