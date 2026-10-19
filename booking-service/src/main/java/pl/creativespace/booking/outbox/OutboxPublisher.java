package pl.creativespace.booking.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * WHY scheduled poller (not CDC): enough for portfolio demos; easy to reason about.
 * Trade-off: small publish lag (fixedDelay) and at-least-once delivery if crash after Kafka ack
 * but before markPublished — consumers must be idempotent.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TraceContextCarrier traceContextCarrier;
    private final String topic;
    private final int batchSize;

    public OutboxPublisher(
            OutboxMessageRepository outboxMessageRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            TraceContextCarrier traceContextCarrier,
            @Value("${app.kafka.booking-events-topic:booking.events}") String topic,
            @Value("${app.outbox.batch-size:50}") int batchSize
    ) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.traceContextCarrier = traceContextCarrier;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxMessage> batch = outboxMessageRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, batchSize));
        for (OutboxMessage message : batch) {
            try {
                traceContextCarrier.runWithRestored(message.getTraceContext(), () -> {
                    try {
                        kafkaTemplate.send(topic, message.getAggregateId().toString(), message.getPayload()).get();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                });
                message.markPublished();
            } catch (Exception ex) {
                log.warn("Outbox publish failed for {}: {}", message.getId(), ex.toString());
                // Stop the batch so ordering for this aggregate stays roughly FIFO; next tick retries.
                break;
            }
        }
    }
}
