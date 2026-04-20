CREATE TABLE equipment (
    id              UUID            NOT NULL PRIMARY KEY,
    type            VARCHAR(50)     NOT NULL,
    brand           VARCHAR(100)    NOT NULL,
    model           VARCHAR(100)    NOT NULL,
    state           VARCHAR(50)     NOT NULL,
    condition_score NUMERIC(3, 2)   NOT NULL,
    purchase_date   DATE            NOT NULL,
    retire_reason   VARCHAR(300),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

CREATE INDEX idx_equipment_state            ON equipment (state);
CREATE INDEX idx_equipment_type             ON equipment (type);
CREATE INDEX idx_equipment_state_type_score ON equipment (state, type, condition_score DESC);

CREATE TABLE allocation_requests (
    id             UUID         NOT NULL PRIMARY KEY,
    employee_id    VARCHAR(255) NOT NULL,
    state          VARCHAR(50)  NOT NULL,
    policy         JSONB        NOT NULL,
    failure_reason TEXT,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_allocation_state    ON allocation_requests (state);
CREATE INDEX idx_allocation_employee ON allocation_requests (employee_id);

CREATE TABLE allocated_equipment (
    allocation_request_id UUID NOT NULL REFERENCES allocation_requests (id),
    equipment_id          UUID NOT NULL REFERENCES equipment (id),
    PRIMARY KEY (allocation_request_id, equipment_id)
);
