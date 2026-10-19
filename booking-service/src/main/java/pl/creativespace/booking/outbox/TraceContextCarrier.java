package pl.creativespace.booking.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures W3C/B3 propagation fields while the HTTP/Feign span is still active,
 * then restores them when the async outbox poller publishes to Kafka.
 */
@Component
public class TraceContextCarrier {

    private final Tracer tracer;
    private final Propagator propagator;
    private final ObjectMapper objectMapper;

    public TraceContextCarrier(Tracer tracer, Propagator propagator, ObjectMapper objectMapper) {
        this.tracer = tracer;
        this.propagator = propagator;
        this.objectMapper = objectMapper;
    }

    public String captureCurrent() {
        TraceContext context = tracer.currentTraceContext().context();
        if (context == null) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(context, carrier, Map::put);
        if (carrier.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(carrier);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize trace context", ex);
        }
    }

    public void runWithRestored(String traceContextJson, Runnable action) {
        if (traceContextJson == null || traceContextJson.isBlank()) {
            action.run();
            return;
        }

        Map<String, String> carrier = readCarrier(traceContextJson);
        Span.Builder extracted = propagator.extract(carrier, (c, key) -> c.get(key));
        Span span = (extracted != null ? extracted : tracer.spanBuilder())
                .name("outbox-publish")
                .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            action.run();
        } finally {
            span.end();
        }
    }

    private Map<String, String> readCarrier(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize trace context", ex);
        }
    }
}
