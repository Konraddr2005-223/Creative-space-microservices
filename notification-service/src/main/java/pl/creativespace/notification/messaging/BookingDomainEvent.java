package pl.creativespace.notification.messaging;

import java.time.Instant;
import java.util.UUID;

public record BookingDomainEvent(
        UUID eventId,
        String type,
        UUID bookingId,
        UUID resourceId,
        String userId,
        Instant occurredAt
) {
}
