# PG location and photo feature removal

The backend and Angular UI no longer expose PG address/city fields or PG photo upload endpoints.
Existing `address` and `city` column values are preserved. The legacy `pg_photos` table is also
left untouched so this change does not destroy stored data.

## Existing MySQL database
1. Back up the database.
2. Run `V4__remove_pg_location_fields.sql` against `pg_management` to make legacy `address` and `city` fields optional.
3. If the database still has legacy `latitude`, `longitude`, or `maps_link` columns that are `NOT NULL`, run `OPTIONAL-MAPS-MIGRATION.sql` too. Skip it if those columns do not exist.
4. Review `OPTIONAL_DASHBOARD_PERFORMANCE_INDEXES.sql` and run only indexes that are not already present.
5. Restart Spring Boot.

The migration makes the legacy address/city columns nullable because the new entity and API no
longer supply them. The optional maps migration does the same for legacy map columns if they exist.
Do not drop old columns unless you explicitly want to permanently delete that legacy data.

## API changes
- Removed `/api/v1/pgs/{pgId}/photos` and related delete/cover endpoints.
- PG create/update request and response fields are now `name`, `type`, `description` and `status`
  (response also includes `id` and timestamps).
- Tenant PG selector displays the PG name only.
