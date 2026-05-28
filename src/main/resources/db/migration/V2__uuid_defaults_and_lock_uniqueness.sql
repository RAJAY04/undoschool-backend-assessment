CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE course ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE teacher ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE parent ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE offering ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE session ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE booking ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE booking_session_lock ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE booking_session_lock
ADD CONSTRAINT uq_lock_parent_session UNIQUE (parent_id, session_id);
