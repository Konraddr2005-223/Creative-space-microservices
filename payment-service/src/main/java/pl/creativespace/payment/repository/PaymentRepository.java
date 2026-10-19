package pl.creativespace.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.creativespace.payment.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBookingId(UUID bookingId);
}
