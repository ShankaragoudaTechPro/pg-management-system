-- Run after backing up pg_management.
-- The application no longer reads or writes PG address/city. Keep existing column values
-- for data preservation, but allow inserts from the new API to omit them.
USE pg_management;

ALTER TABLE pg_properties
    MODIFY COLUMN address VARCHAR(500) NULL,
    MODIFY COLUMN city VARCHAR(100) NULL;

-- The pg_photos table is intentionally not dropped here; old photo records are preserved.
-- The application no longer maps this table or exposes photo endpoints.
