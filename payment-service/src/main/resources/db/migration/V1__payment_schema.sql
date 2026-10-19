CREATE TABLE payments (
    id              UUID PRIMARY KEY,
    booking_id      UUID           NOT NULL UNIQUE,
    user_id         VARCHAR(128)   NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        CHAR(3)        NOT NULL,
    status          VARCHAR(32)    NOT NULL,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_payments_status ON payments (status);
