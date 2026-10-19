package pl.creativespace.payment.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.creativespace.payment.domain.PaymentStatus;
import pl.creativespace.payment.dto.ChargePaymentRequest;
import pl.creativespace.payment.dto.PaymentResponse;
import pl.creativespace.payment.service.PaymentChargeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentChargeService paymentChargeService;

    public PaymentController(PaymentChargeService paymentChargeService) {
        this.paymentChargeService = paymentChargeService;
    }

    /**
     * WHY 402 on FAILED (not 201): Feign sees a non-2xx and the saga compensates.
     * Body still carries the persisted payment so demos can inspect failureReason.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> charge(
            @Valid @RequestBody ChargePaymentRequest request,
            @RequestHeader(value = PaymentChargeService.FORCE_FAILURE_HEADER, required = false)
            String forceFailureHeader
    ) {
        boolean forceFailure = Boolean.parseBoolean(forceFailureHeader);
        PaymentResponse response = paymentChargeService.charge(request, forceFailure);
        if (response.status() == PaymentStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/by-booking/{bookingId}")
    public PaymentResponse getByBooking(@PathVariable UUID bookingId) {
        return paymentChargeService.getByBookingId(bookingId);
    }
}
