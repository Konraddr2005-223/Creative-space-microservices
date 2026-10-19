package pl.creativespace.resource.domain;

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
import java.util.UUID;

@Entity
@Table(name = "price_tiers")
public class PriceTier {

    public enum PricingUnit {
        HOUR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PricingUnit unit = PricingUnit.HOUR;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "PLN";

    private Instant validFrom;

    private Instant validTo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected PriceTier() {
    }

    public PriceTier(Resource resource, PricingUnit unit, BigDecimal amount, String currency,
                     Instant validFrom, Instant validTo) {
        this.resource = resource;
        this.unit = unit;
        this.amount = amount;
        this.currency = currency;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Resource getResource() {
        return resource;
    }

    public PricingUnit getUnit() {
        return unit;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
