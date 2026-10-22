> **Current status:** This file contains historical integration notes. The latest code changes and current verification limits are documented in `../../REPAIR-REPORT.md`. Use that report as the source of truth.

# PG Management Angular / Spring Boot integration progress

## Included in this package

- Tenant DTO compatibility fields for the Angular tenant UI (`fullName`, `mobileNumber`, and status aliases), while preserving legacy backend fields.
- Tenant list response compatibility: no query parameters returns an array for the feature UI; search/filter/pagination parameters retain a paginated response.
- Notification list response compatibility: no query parameters returns an array for the Angular notification UI; explicit pagination or read filters retain a paginated response.
- Aadhaar is not exposed in tenant response DTOs.

## Important limitations

- This is an incremental compatibility package, not a completed integration.
- Room CRUD compatibility, tenant update/delete, and profile component integration remain to be implemented.
- The Angular tenant form sends PG/room/bed labels, while the backend allocation model requires real database IDs. Do not bypass those relationships or guess an allocation.
- Build/runtime verification is pending because the Maven wrapper distribution download failed in the available environment. Run `mvnw.cmd -DskipTests compile` on Windows or `./mvnw -DskipTests compile` where Maven is available, then test with a real owner JWT and database.
