package pl.creativespace.booking.saga;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.booking.client.dto.ChargePaymentRequest;
import pl.creativespace.booking.client.dto.PaymentResponse;
import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.domain.BookingStatus;
import pl.creativespace.booking.dto.BookingResponse;
import pl.creativespace.booking.dto.CheckoutRequest;
import pl.creativespace.booking.repository.BookingRepository;
import pl.creativespace.booking.service.BookingService;

import java.util.UUID;

/**
 * Orchestrated saga (sync Feign step):
 * PENDING booking → charge payment → CONFIRMED on success / CANCELLED on failure.
 * <p>
 * WHY no @Transactional around Feign: holding a DB connection during HTTP is a classic
 * pool-exhaustion footgun under load. confirm/cancel run in their own short transactions.
 * <p>
 * WHY orchestration: booking owns the process; payment is a subordinate step with a clear
 * compensating action (cancel → frees EXCLUDE slot).
 */
@Service
public class BookingCheckoutSaga {

    private static final Logger log = LoggerFactory.getLogger(BookingCheckoutSaga.class);

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final PaymentGateway paymentGateway;

    public BookingCheckoutSaga(
            BookingRepository bookingRepository,
            BookingService bookingService,
            PaymentGateway paymentGateway
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.paymentGateway = paymentGateway;
    }

    public BookingResponse checkout(UUID bookingId, CheckoutRequest request, boolean forcePaymentFailure) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return BookingResponse.from(booking);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Checkout requires PENDING booking, was: " + booking.getStatus()
            );
        }

        try {
            PaymentResponse payment = paymentGateway.charge(
                    new ChargePaymentRequest(
                            booking.getId(),
                            booking.getUserId(),
                            request.amount(),
                            request.currency().toUpperCase()
                    ),
                    forcePaymentFailure ? "true" : null
            );

            if (!"SUCCEEDED".equalsIgnoreCase(payment.status())) {
                return compensateAndFail(bookingId, "Payment status was " + payment.status());
            }
            return bookingService.confirm(bookingId);
        } catch (FeignException ex) {
            // Includes HTTP 402 from forced payment failure.
            log.warn("Payment call failed for booking {}: HTTP {}", bookingId, ex.status());
            return compensateAndFail(bookingId, "Payment service rejected charge (HTTP " + ex.status() + ")");
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                return compensateAndFail(bookingId, ex.getReason());
            }
            throw ex;
        }
    }

    private BookingResponse compensateAndFail(UUID bookingId, String reason) {
        log.info("Compensating booking {}: {}", bookingId, reason);
        bookingService.cancel(bookingId);
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Checkout failed and booking was cancelled. Reason: " + reason
        );
    }
}
