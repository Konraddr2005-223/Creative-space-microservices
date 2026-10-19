package pl.creativespace.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.domain.BookingStatus;
import pl.creativespace.booking.dto.CreateBookingRequest;
import pl.creativespace.booking.repository.BookingRepository;
import pl.creativespace.booking.service.BookingService;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingServiceUnitTest {

    @Mock
    BookingRepository bookingRepository;

    @InjectMocks
    BookingService bookingService;

    @Test
    void createRejectsInvalidRange() {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(),
                "user-1",
                Instant.parse("2026-06-01T12:00:00Z"),
                Instant.parse("2026-06-01T11:00:00Z")
        );

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("endAt must be after startAt");

        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void confirmRejectsNonPending() {
        Booking booking = new Booking(
                UUID.randomUUID(),
                "user-1",
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T11:00:00Z"),
                BookingStatus.CANCELLED
        );

        assertThatThrownBy(booking::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING");
    }
}
