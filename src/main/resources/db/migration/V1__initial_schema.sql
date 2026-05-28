-- V1__initial_schema.sql

-- 1. Course Table
CREATE TABLE course (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT
);

-- 2. Teacher Table
CREATE TABLE teacher (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    timezone VARCHAR(100) NOT NULL
);

-- 3. Parent / Student Table
CREATE TABLE parent (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    timezone VARCHAR(100) NOT NULL
);

-- 4. Offering (Section) Table
CREATE TABLE offering (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    max_students INTEGER NOT NULL CHECK (max_students > 0),
    current_enrollment INTEGER NOT NULL DEFAULT 0 CHECK (current_enrollment >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offering_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE RESTRICT,
    CONSTRAINT fk_offering_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE RESTRICT,
    CONSTRAINT chk_enrollment_cap CHECK (current_enrollment <= max_students)
);

-- 5. Session Table
CREATE TABLE session (
    id UUID PRIMARY KEY,
    offering_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_session_offering FOREIGN KEY (offering_id) REFERENCES offering(id) ON DELETE RESTRICT,
    CONSTRAINT chk_session_times CHECK (end_time > start_time)
);

-- 6. Booking Table
CREATE TABLE booking (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL,
    offering_id UUID NOT NULL,
    booked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_parent FOREIGN KEY (parent_id) REFERENCES parent(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_offering FOREIGN KEY (offering_id) REFERENCES offering(id) ON DELETE RESTRICT,
    CONSTRAINT uq_parent_offering UNIQUE (parent_id, offering_id)
);

-- 7. Booking Session Lock (Denormalized table for fast conflict checks)
CREATE TABLE booking_session_lock (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    session_id UUID NOT NULL,
    parent_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lock_booking FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_lock_session FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE RESTRICT,
    CONSTRAINT fk_lock_parent FOREIGN KEY (parent_id) REFERENCES parent(id) ON DELETE RESTRICT,
    CONSTRAINT chk_lock_times CHECK (end_time > start_time)
);

-- Index for searching sessions belonging to an offering
CREATE INDEX idx_session_offering_id ON session(offering_id);

-- Index for checking parents' booking histories
CREATE INDEX idx_booking_parent_id ON booking(parent_id);

-- Index for checking enrollment levels for an offering
CREATE INDEX idx_booking_offering_id ON booking(offering_id);

-- Composite Index for fast parent conflict checking
-- This allows an index-only scan (or fast index scan) for overlapping ranges:
-- SELECT 1 FROM booking_session_lock WHERE parent_id = ? AND start_time < ? AND end_time > ?
CREATE INDEX idx_lock_parent_times ON booking_session_lock(parent_id, start_time, end_time);
