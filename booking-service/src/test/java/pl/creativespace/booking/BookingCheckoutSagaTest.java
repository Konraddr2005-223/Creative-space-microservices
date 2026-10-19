package pl.creativespace.booking;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.booking.client.dto.ChargePaymentRequest;
import pl.creativespace.booking.client.dto.PaymentResponse;
import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.domain.BookingStatus;
import pl.creativespace.booking.dto.BookingResponse;
import pl.creativespace.booking.dto.CheckoutRequest;
import pl.creativespace.booking.repository.BookingRepository;
import pl.creativespace.booking.saga.BookingCheckoutSaga;
import pl.creativespace.booking.saga.PaymentGateway;
import pl.creativespace.booking.service.BookingService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCheckoutSagaTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    BookingService bookingService;

    @Mock
    PaymentGateway paymentGateway;

    @InjectMocks
    BookingCheckoutSaga checkoutSaga;

    @Test
    void successfulPaymentConfirmsBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Booking pending = bookingWithId(
                bookingId,
                UUID.randomUUID(), "demo",
                Instant.parse("2026-08-01T10:00:00Z"),
                Instant.parse("2026-08-01T11:00:00Z"),
                BookingStatus.PENDING
        );
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pending));
        when(paymentGateway.charge(any(ChargePaymentRequest.class), eq(null)))
                .thenReturn(new PaymentResponse(
                        UUID.randomUUID(), bookingId, "demo",
                        new BigDecimal("100.00"), "PLN", "SUCCEEDED", null, Instant.now()
                ));
        when(bookingService.confirm(bookingId)).thenReturn(
                new BookingResponse(
                        bookingId, pending.getResourceId(), "demo",
                        pending.getStartAt(), pending.getEndAt(),
                        BookingStatus.CONFIRMED, Instant.now(), Instant.now(), 1
                )
        );

        BookingResponse response = checkoutSaga.checkout(
                bookingId, new CheckoutRequest(new BigDecimal("100.00"), "PLN"), false
        );

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingService).confirm(bookingId);
    }

    @Test
    void forcedPaymentFailureCancelsBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Booking pending = bookingWithId(
                bookingId,
                UUID.randomUUID(), "demo",
                Instant.parse("2026-08-01T10:00:00Z"),
                Instant.parse("2026-08-01T11:00:00Z"),
                BookingStatus.PENDING
        );
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pending));

        Request request = Request.create(
                Request.HttpMethod.POST, "/api/payments", Collections.emptyMap(),
                null, StandardCharsets.UTF_8, new RequestTemplate()
        );
        when(paymentGateway.charge(any(ChargePaymentRequest.class), eq("true")))
                .thenThrow(new FeignException.FeignClientException(
                        402, "Payment Required", request, null, Collections.emptyMap()
                ));
        when(bookingService.cancel(bookingId)).thenReturn(
                new BookingResponse(
                        bookingId, pending.getResourceId(), "demo",
                        pending.getStartAt(), pending.getEndAt(),
                        BookingStatus.CANCELLED, Instant.now(), Instant.now(), 1
                )
        );

        assertThatThrownBy(() -> checkoutSaga.checkout(
                bookingId, new CheckoutRequest(new BigDecimal("100.00"), "PLN"), true
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("cancelled");
                });

        verify(bookingService).cancel(bookingId);
    }

    private static Booking bookingWithId(
            UUID id, UUID resourceId, String userId, Instant start, Instant end, BookingStatus status
    ) throws Exception {
        Booking booking = new Booking(resourceId, userId, start, end, status);
        var idField = Booking.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(booking, id);
        return booking;
    }
}
