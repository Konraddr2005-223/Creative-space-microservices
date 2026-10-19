package pl.creativespace.booking.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.messaging.BookingDomainEvent;
import pl.creativespace.booking.messaging.BookingEventType;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    public static final String AGGREGATE_BOOKING = "Booking";

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final TraceContextCarrier traceContextCarrier;

    public OutboxService(
            OutboxMessageRepository outboxMessageRepository,
            ObjectMapper objectMapper,
            TraceContextCarrier traceContextCarrier
    ) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
        this.traceContextCarrier = traceContextCarrier;
    }

    /**
     * Must run in the same transaction that mutates the booking.
     * eventId is the outbox PK and the consumer idempotency key.
     */
    @Transactional
    public void enqueue(Booking booking, BookingEventType type) {
        UUID eventId = UUID.randomUUID();
        BookingDomainEvent event = new BookingDomainEvent(
                eventId,
                type,
                booking.getId(),
                booking.getResourceId(),
                booking.getUserId(),
                Instant.now()
        );

        outboxMessageRepository.save(new OutboxMessage(
                eventId,
                AGGREGATE_BOOKING,
                booking.getId(),
                type.name(),
                toJson(event),
                traceContextCarrier.captureCurrent()
        ));
    }

    private String toJson(BookingDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize booking event", ex);
        }
    }
}
