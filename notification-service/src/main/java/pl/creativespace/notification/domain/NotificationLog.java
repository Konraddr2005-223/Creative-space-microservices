package pl.creativespace.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, length = 160)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationLog() {
    }

    public NotificationLog(UUID eventId, String channel, String recipient, String body) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.channel = channel;
        this.recipient = recipient;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getBody() {
        return body;
    }
}
