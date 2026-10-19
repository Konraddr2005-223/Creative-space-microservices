package pl.creativespace.booking.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire format shared with notification-service (JSON).
 * eventId == outbox message id for end-to-end idempotency.
 */
public record BookingDomainEvent(
        UUID eventId,
        BookingEventType type,
        UUID bookingId,
        UUID resourceId,
        String userId,
        Instant occurredAt
) {
}
