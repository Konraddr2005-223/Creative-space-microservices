-- WHY btree_gist: enables EXCLUDE constraints that mix equality (=) and range overlap (&&).
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
    id          UUID PRIMARY KEY,
    resource_id UUID         NOT NULL,
    user_id     VARCHAR(128) NOT NULL,
    start_at    TIMESTAMPTZ  NOT NULL,
    end_at      TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT bookings_valid_range CHECK (end_at > start_at)
);

CREATE INDEX idx_bookings_resource_id ON bookings (resource_id);
CREATE INDEX idx_bookings_status ON bookings (status);

-- WHY EXCLUDE (not app-level SELECT-then-INSERT):
-- Two concurrent transactions can both pass a "no overlap" SELECT and both INSERT.
-- The DB constraint serializes the conflict at commit time — exactly one winner.
-- Half-open range [start, end) so back-to-back slots (10-11 and 11-12) do not collide.
-- WHERE clause: CANCELLED rows free the slot for reuse.
ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'CONFIRMED'));
