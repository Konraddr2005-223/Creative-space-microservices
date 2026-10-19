-- WHY outbox in the same DB as bookings:
-- insert booking state change + outbox row in ONE transaction → no "committed booking, lost event".
CREATE TABLE outbox_messages (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_messages (created_at) WHERE published_at IS NULL;
