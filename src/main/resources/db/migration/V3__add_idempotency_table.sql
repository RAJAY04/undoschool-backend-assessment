CREATE TABLE idempotent_request (
    key VARCHAR(255) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    response_status INT,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_idempotent_request_created_at ON idempotent_request(created_at);

-- Foreign key indexes to optimize teacher dashboard reads and cascade deletes
CREATE INDEX idx_offering_teacher_id ON offering(teacher_id);
CREATE INDEX idx_lock_booking_id ON booking_session_lock(booking_id);
