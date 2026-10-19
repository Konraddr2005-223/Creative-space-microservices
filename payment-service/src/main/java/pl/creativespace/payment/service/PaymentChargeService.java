package pl.creativespace.payment.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.payment.domain.Payment;
import pl.creativespace.payment.domain.PaymentStatus;
import pl.creativespace.payment.dto.ChargePaymentRequest;
import pl.creativespace.payment.dto.PaymentResponse;
import pl.creativespace.payment.repository.PaymentRepository;

import java.util.UUID;

@Service
public class PaymentChargeService {

    public static final String FORCE_FAILURE_HEADER = "X-Force-Payment-Failure";

    private final PaymentRepository paymentRepository;

    public PaymentChargeService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * WHY idempotent by bookingId:
     * Saga retries (Feign timeout → retry) must not create a second charge for the same booking.
     * First result wins; subsequent calls return the stored outcome.
     */
    @Transactional
    public PaymentResponse charge(ChargePaymentRequest request, boolean forceFailure) {
        return paymentRepository.findByBookingId(request.bookingId())
                .map(PaymentResponse::from)
                .orElseGet(() -> createNewCharge(request, forceFailure));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByBookingId(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found for booking: " + bookingId));
    }

    private PaymentResponse createNewCharge(ChargePaymentRequest request, boolean forceFailure) {
        if (forceFailure) {
            Payment failed = new Payment(
                    request.bookingId(),
                    request.userId(),
                    request.amount(),
                    request.currency().toUpperCase(),
                    PaymentStatus.FAILED,
                    "Forced failure via " + FORCE_FAILURE_HEADER
            );
            return PaymentResponse.from(paymentRepository.save(failed));
        }

        Payment succeeded = new Payment(
                request.bookingId(),
                request.userId(),
                request.amount(),
                request.currency().toUpperCase(),
                PaymentStatus.SUCCEEDED,
                null
        );
        return PaymentResponse.from(paymentRepository.save(succeeded));
    }
}
