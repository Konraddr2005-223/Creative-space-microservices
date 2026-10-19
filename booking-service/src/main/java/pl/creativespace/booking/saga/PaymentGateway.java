package pl.creativespace.booking.saga;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.booking.client.PaymentClient;
import pl.creativespace.booking.client.dto.ChargePaymentRequest;
import pl.creativespace.booking.client.dto.PaymentResponse;

/**
 * Separate bean so Resilience4j proxies apply (self-invocation inside saga would skip aspects).
 */
@Component
public class PaymentGateway {

    private final PaymentClient paymentClient;

    public PaymentGateway(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "chargeFallback")
    @Retry(name = "payment")
    public PaymentResponse charge(ChargePaymentRequest request, String forceFailure) {
        return paymentClient.charge(request, forceFailure);
    }

    @SuppressWarnings("unused")
    private PaymentResponse chargeFallback(ChargePaymentRequest request, String forceFailure, Throwable cause) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Payment circuit open or retries exhausted: " + cause.getClass().getSimpleName(),
                cause
        );
    }
}
