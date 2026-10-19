package pl.creativespace.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import pl.creativespace.booking.client.dto.ChargePaymentRequest;
import pl.creativespace.booking.client.dto.PaymentResponse;

/**
 * WHY url property: empty → Eureka discovery; tests set payment.service.url to a WireMock/stub.
 */
@FeignClient(name = "payment-service", url = "${payment.service.url:}")
public interface PaymentClient {

    @PostMapping("/api/payments")
    PaymentResponse charge(
            @RequestBody ChargePaymentRequest request,
            @RequestHeader(value = "X-Force-Payment-Failure", required = false) String forceFailure
    );
}
