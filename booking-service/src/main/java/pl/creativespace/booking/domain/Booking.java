package pl.creativespace.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID resourceId;

    /**
     * WHY String (not UUID): Keycloak subject is an opaque string; keep the edge identity as-is.
     */
    @Column(nullable = false, length = 128)
    private String userId;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * WHY @Version exists here (and what it does NOT do):
     * Optimistic locking protects concurrent UPDATES of the same row (e.g. two cancel calls).
     * It does NOT stop two concurrent INSERTs of overlapping ranges — different rows, both version=0.
     * Overlap safety is the PostgreSQL EXCLUDE constraint on (resource_id, tstzrange).
     */
    @Version
    private long version;

    protected Booking() {
    }

    public Booking(UUID resourceId, String userId, Instant startAt, Instant endAt, BookingStatus status) {
        this.resourceId = resourceId;
        this.userId = userId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
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

    public void cancel() {
        if (status == BookingStatus.CANCELLED) {
            return;
        }
        this.status = BookingStatus.CANCELLED;
    }

    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed, was: " + status);
        }
        this.status = BookingStatus.CONFIRMED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
