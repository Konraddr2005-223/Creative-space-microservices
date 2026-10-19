package pl.creativespace.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.creativespace.payment.domain.Payment;
import pl.creativespace.payment.domain.PaymentStatus;
import pl.creativespace.payment.dto.ChargePaymentRequest;
import pl.creativespace.payment.dto.PaymentResponse;
import pl.creativespace.payment.repository.PaymentRepository;
import pl.creativespace.payment.service.PaymentChargeService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentChargeServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks
    PaymentChargeService paymentChargeService;

    @Test
    void forceFailurePersistsFailedPayment() {
        UUID bookingId = UUID.randomUUID();
        ChargePaymentRequest request = new ChargePaymentRequest(
                bookingId, "demo", new BigDecimal("100.00"), "PLN"
        );
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentChargeService.charge(request, true);

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failureReason()).contains("X-Force-Payment-Failure");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void secondChargeIsIdempotent() {
        UUID bookingId = UUID.randomUUID();
        Payment existing = new Payment(
                bookingId, "demo", new BigDecimal("50.00"), "PLN",
                PaymentStatus.SUCCEEDED, null
        );
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existing));

        PaymentResponse response = paymentChargeService.charge(
                new ChargePaymentRequest(bookingId, "demo", new BigDecimal("50.00"), "PLN"),
                false
        );

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(paymentRepository, never()).save(any());
    }
}
