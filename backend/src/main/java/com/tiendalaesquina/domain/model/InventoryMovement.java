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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 32)
    private InventoryMovementType movementType;

    @Column(name = "quantity_delta", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityDelta;

    @Column(name = "quantity_before", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private InventorySourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserAccount createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryMovement() {
    }

    public InventoryMovement(Product product, InventoryMovementType movementType,
                             BigDecimal quantityDelta, BigDecimal quantityBefore,
                             BigDecimal quantityAfter, InventorySourceType sourceType,
                             Long sourceId, String reason, UserAccount createdBy,
                             Instant createdAt) {
        this.product = product;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public InventoryMovementType getMovementType() {
        return movementType;
    }

    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public InventorySourceType getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getReason() {
        return reason;
    }

    public UserAccount getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
