-- WHY store processed event ids:
-- Kafka is at-least-once. Replaying the same outbox row must not double-notify.
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(64)  NOT NULL,
    booking_id   UUID         NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE notification_log (
    id           UUID PRIMARY KEY,
    event_id     UUID         NOT NULL UNIQUE,
    channel      VARCHAR(32)  NOT NULL,
    recipient    VARCHAR(160) NOT NULL,
    body         VARCHAR(500) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);
