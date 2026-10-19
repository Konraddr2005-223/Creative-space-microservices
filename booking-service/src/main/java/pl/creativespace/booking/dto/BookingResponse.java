package pl.creativespace.booking.dto;

import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.domain.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID resourceId,
        String userId,
        Instant startAt,
        Instant endAt,
        BookingStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getResourceId(),
                booking.getUserId(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getVersion()
        );
    }
}
