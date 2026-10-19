package pl.creativespace.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID resourceId,
        @NotBlank @Size(max = 128) String userId,
        @NotNull Instant startAt,
        @NotNull Instant endAt
) {
}
