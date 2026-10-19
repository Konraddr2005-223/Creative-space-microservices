package pl.creativespace.booking.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargePaymentRequest(
        UUID bookingId,
        String userId,
        BigDecimal amount,
        String currency
) {
}
