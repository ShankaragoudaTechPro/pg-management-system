-- Backward-compatible migration for legacy map columns.
-- Map fields are no longer part of the Angular UI or PG create/update API.
-- Preserve the columns and existing values, but allow inserts that omit them.
-- Back up pg_management before applying this migration.
USE pg_management;

ALTER TABLE pg_properties
    MODIFY COLUMN latitude DOUBLE NULL,
    MODIFY COLUMN longitude DOUBLE NULL,
    MODIFY COLUMN maps_link TEXT NULL;
