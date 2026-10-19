package pl.creativespace.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.creativespace.notification.service.NotificationHandler;

@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationHandler notificationHandler;

    public BookingEventListener(ObjectMapper objectMapper, NotificationHandler notificationHandler) {
        this.objectMapper = objectMapper;
        this.notificationHandler = notificationHandler;
    }

    @KafkaListener(topics = "${app.kafka.booking-events-topic:booking.events}", groupId = "notification-service")
    public void onMessage(String payload) {
        try {
            BookingDomainEvent event = objectMapper.readValue(payload, BookingDomainEvent.class);
            notificationHandler.handle(event);
        } catch (Exception ex) {
            log.error("Failed to process booking event payload: {}", ex.toString());
            // Rethrow so the container can retry / DLQ later; for now default error handler retries.
            throw new IllegalStateException("Booking event processing failed", ex);
        }
    }
}
