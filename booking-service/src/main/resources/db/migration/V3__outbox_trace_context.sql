-- WHY store propagation map on the outbox row:
-- the HTTP/Feign span ends before the async poller publishes to Kafka.
-- Without this, Zipkin would show a disconnected Kafka trace.
ALTER TABLE outbox_messages
    ADD COLUMN trace_context TEXT;
