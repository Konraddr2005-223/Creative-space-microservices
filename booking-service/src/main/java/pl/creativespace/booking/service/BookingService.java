package pl.creativespace.booking.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.domain.BookingStatus;
import pl.creativespace.booking.dto.BookingResponse;
import pl.creativespace.booking.dto.CreateBookingRequest;
import pl.creativespace.booking.repository.BookingRepository;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
        }

        Booking booking = new Booking(
                request.resourceId(),
                request.userId(),
                request.startAt(),
                request.endAt(),
                BookingStatus.PENDING
        );

        try {
            // flush so EXCLUDE fires inside this transaction (not only at commit after response mapping)
            return BookingResponse.from(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException ex) {
            throw translateOverlap(ex);
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID id) {
        return BookingResponse.from(requireBooking(id));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listByResource(UUID resourceId, boolean activeOnly) {
        List<Booking> bookings = activeOnly
                ? bookingRepository.findByResourceIdAndStatusInOrderByStartAtAsc(
                resourceId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                : bookingRepository.findByResourceIdOrderByStartAtAsc(resourceId);
        return bookings.stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse cancel(UUID id) {
        Booking booking = requireBooking(id);
        booking.cancel();
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse confirm(UUID id) {
        Booking booking = requireBooking(id);
        try {
            booking.confirm();
            return BookingResponse.from(bookingRepository.saveAndFlush(booking));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private Booking requireBooking(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + id));
    }

    /**
     * WHY map SQLState 23P01 → 409:
     * PostgreSQL exclusion_violation means another active booking already covers this range.
     * That is a business conflict, not a 500.
     */
    private static ResponseStatusException translateOverlap(DataIntegrityViolationException ex) {
        Throwable root = ex.getMostSpecificCause();
        String message = root != null ? root.getMessage() : ex.getMessage();
        if (message != null && (message.contains("bookings_no_overlap") || message.contains("23P01"))) {
            return new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Resource already booked for an overlapping time range"
            );
        }
        return new ResponseStatusException(HttpStatus.CONFLICT, "Booking conflicts with an existing constraint");
    }
}
