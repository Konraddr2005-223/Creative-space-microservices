package pl.creativespace.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.creativespace.notification.domain.NotificationLog;
import pl.creativespace.notification.domain.ProcessedEvent;
import pl.creativespace.notification.messaging.BookingDomainEvent;
import pl.creativespace.notification.repository.NotificationLogRepository;
import pl.creativespace.notification.repository.ProcessedEventRepository;
import pl.creativespace.notification.service.NotificationHandler;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationHandlerTest {

    @Mock
    ProcessedEventRepository processedEventRepository;

    @Mock
    NotificationLogRepository notificationLogRepository;

    @InjectMocks
    NotificationHandler notificationHandler;

    @Test
    void firstDeliveryPersistsNotification() {
        UUID eventId = UUID.randomUUID();
        BookingDomainEvent event = new BookingDomainEvent(
                eventId, "BOOKING_CONFIRMED", UUID.randomUUID(), UUID.randomUUID(),
                "demo", Instant.now()
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        notificationHandler.handle(event);

        verify(notificationLogRepository).save(any(NotificationLog.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void duplicateDeliveryIsSkipped() {
        UUID eventId = UUID.randomUUID();
        BookingDomainEvent event = new BookingDomainEvent(
                eventId, "BOOKING_CANCELLED", UUID.randomUUID(), UUID.randomUUID(),
                "demo", Instant.now()
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        notificationHandler.handle(event);

        verify(notificationLogRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }
}
