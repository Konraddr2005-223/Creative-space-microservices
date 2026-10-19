package pl.creativespace.booking.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason,
        Instant createdAt
) {
}
