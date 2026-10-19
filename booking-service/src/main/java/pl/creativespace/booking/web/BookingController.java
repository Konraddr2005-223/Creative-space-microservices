package pl.creativespace.booking.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.creativespace.booking.dto.BookingResponse;
import pl.creativespace.booking.dto.CheckoutRequest;
import pl.creativespace.booking.dto.CreateBookingRequest;
import pl.creativespace.booking.saga.BookingCheckoutSaga;
import pl.creativespace.booking.service.BookingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingCheckoutSaga checkoutSaga;

    public BookingController(BookingService bookingService, BookingCheckoutSaga checkoutSaga) {
        this.bookingService = bookingService;
        this.checkoutSaga = checkoutSaga;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable UUID id) {
        return bookingService.get(id);
    }

    @GetMapping
    public List<BookingResponse> listByResource(
            @RequestParam UUID resourceId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return bookingService.listByResource(resourceId, activeOnly);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id) {
        return bookingService.cancel(id);
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(@PathVariable UUID id) {
        return bookingService.confirm(id);
    }

    /**
     * Orchestrated checkout: charge payment-service then confirm, or cancel on failure.
     * Pass X-Force-Payment-Failure: true to demo compensation without a real payment outage.
     */
    @PostMapping("/{id}/checkout")
    public BookingResponse checkout(
            @PathVariable UUID id,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Force-Payment-Failure", required = false) String forceFailure
    ) {
        return checkoutSaga.checkout(id, request, Boolean.parseBoolean(forceFailure));
    }
}
