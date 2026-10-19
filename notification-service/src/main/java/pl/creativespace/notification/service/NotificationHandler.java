package pl.creativespace.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.creativespace.notification.domain.NotificationLog;
import pl.creativespace.notification.domain.ProcessedEvent;
import pl.creativespace.notification.messaging.BookingDomainEvent;
import pl.creativespace.notification.repository.NotificationLogRepository;
import pl.creativespace.notification.repository.ProcessedEventRepository;

import java.time.Instant;

@Service
public class NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationHandler(
            ProcessedEventRepository processedEventRepository,
            NotificationLogRepository notificationLogRepository
    ) {
        this.processedEventRepository = processedEventRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    /**
     * Idempotent handle: first insert into processed_events wins.
     * Unique PK on event_id makes concurrent duplicates safe under at-least-once delivery.
     */
    @Transactional
    public void handle(BookingDomainEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate booking event {}", event.eventId());
            return;
        }

        String body = "Booking " + event.bookingId() + " " + event.type()
                + " for user " + event.userId();
        notificationLogRepository.save(new NotificationLog(
                event.eventId(),
                "LOG",
                event.userId(),
                body
        ));
        processedEventRepository.save(new ProcessedEvent(
                event.eventId(),
                event.type(),
                event.bookingId(),
                Instant.now()
        ));
        log.info("Notification recorded: {}", body);
    }
}
