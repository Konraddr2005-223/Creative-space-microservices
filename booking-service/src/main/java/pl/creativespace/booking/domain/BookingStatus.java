package pl.creativespace.booking.domain;

/**
 * PENDING/CONFIRMED occupy the calendar (EXCLUDE applies).
 * CANCELLED frees the slot so another booking can reuse the range.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
