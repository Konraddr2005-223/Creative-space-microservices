package pl.creativespace.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.creativespace.booking.domain.Booking;
import pl.creativespace.booking.domain.BookingStatus;
import pl.creativespace.booking.messaging.BookingEventType;
import pl.creativespace.booking.outbox.OutboxMessage;
import pl.creativespace.booking.outbox.OutboxMessageRepository;
import pl.creativespace.booking.outbox.OutboxService;
import pl.creativespace.booking.outbox.TraceContextCarrier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    OutboxMessageRepository outboxMessageRepository;

    @Mock
    TraceContextCarrier traceContextCarrier;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    OutboxService outboxService;

    @Test
    void enqueuePersistsPayloadWithMatchingEventId() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking(
                UUID.randomUUID(), "demo",
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-01T11:00:00Z"),
                BookingStatus.CONFIRMED
        );
        var idField = Booking.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(booking, bookingId);
        when(traceContextCarrier.captureCurrent()).thenReturn("{\"traceparent\":\"00-abc-def-01\"}");

        outboxService.enqueue(booking, BookingEventType.BOOKING_CONFIRMED);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());
        OutboxMessage saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(saved.getAggregateId()).isEqualTo(bookingId);
        assertThat(saved.getPayload()).contains(bookingId.toString());
        assertThat(saved.getPayload()).contains(saved.getId().toString());
        assertThat(saved.getTraceContext()).contains("traceparent");
    }
}
