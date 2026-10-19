package pl.creativespace.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.domain.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByResourceIdOrderByStartAtAsc(UUID resourceId);

    List<Booking> findByResourceIdAndStatusInOrderByStartAtAsc(UUID resourceId, List<BookingStatus> statuses);
}
