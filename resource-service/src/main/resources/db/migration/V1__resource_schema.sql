CREATE TABLE resources (
    id          UUID PRIMARY KEY,
    name        VARCHAR(160)  NOT NULL,
    type        VARCHAR(32)   NOT NULL,
    description VARCHAR(1000),
    location    VARCHAR(160)  NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_resources_type ON resources (type);
CREATE INDEX idx_resources_active ON resources (active);

CREATE TABLE price_tiers (
    id          UUID PRIMARY KEY,
    resource_id UUID          NOT NULL REFERENCES resources (id),
    unit        VARCHAR(16)   NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL,
    currency    CHAR(3)       NOT NULL,
    valid_from  TIMESTAMPTZ,
    valid_to    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_price_tiers_resource_id ON price_tiers (resource_id);
