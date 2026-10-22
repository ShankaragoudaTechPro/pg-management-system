-- Optional dashboard indexes for MySQL.
-- Back up first and inspect SHOW INDEX FROM each table. Do not create an index if an
-- equivalent leading-column index already exists; CREATE INDEX is intentionally not
-- IF NOT EXISTS because MySQL version support differs.
USE pg_management;

CREATE INDEX idx_rooms_pg_active ON rooms (pg_id, active);
CREATE INDEX idx_beds_room_status ON beds (room_id, status);
CREATE INDEX idx_allocations_pg_status ON tenant_allocations (pg_id, status);
CREATE INDEX idx_rent_records_pg_status_due ON rent_records (pg_id, status, due_date);
