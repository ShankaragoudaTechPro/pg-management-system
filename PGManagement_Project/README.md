# PG Management System

A web application for managing paying guest (PG) accommodation operations. This repository contains an Angular frontend and a Spring Boot REST API backed by MySQL.

> **Project status:** Under active development. See `pg-management-backend/INTEGRATION-NOTES.md` for documented integration limitations. Features should not be considered production-ready until tested end to end.

## Technology stack
- Frontend: Angular 21, TypeScript, Tailwind CSS, Angular SSR
- Backend: Java 17, Spring Boot, Spring Security, Spring Data JPA, JWT
- Database: MySQL
- Build tools: npm and Maven

## Main capabilities
The codebase includes modules and code for owner authentication, PG properties and facilities, rooms/beds, tenants, rent tracking, dashboard data, and notifications. Verify completeness and runtime behavior against the current code and database.

## Repository structure
```text
PGManagement_Project/
├── pg-management-frontend/
├── pg-management-backend/
│   ├── INTEGRATION-NOTES.md
│   └── pg-management/
├── Dockerfile.frontend
├── Dockerfile.backend
├── docker-compose.yml
├── .dockerignore
├── .gitignore
├── .env.example
└── README.md
```

## Prerequisites
- JDK 17
- Node.js compatible with Angular 21 (Docker build uses Node 24)
- npm
- MySQL 8.x for local development
- Maven or the included Maven Wrapper

## Local setup

### 1. Configure MySQL
Create a local database named `pg_management`. Configure the environment variables used by the backend. Never commit real passwords, JWT secrets, SMS credentials, or personal data.

### 2. Run the backend
From `pg-management-backend/pg-management` on Windows PowerShell:
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="pg_management"
$env:DB_USERNAME="your_mysql_username"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="replace_with_a_long_random_secret"
.\mvnw.cmd spring-boot:run
```
You can use `mvn spring-boot:run` if Maven is installed globally.

### 3. Run the frontend
From `pg-management-frontend`:
```bash
npm ci
npm start
```
The development server normally runs at `http://localhost:4200`.

## Docker local demo
1. Copy `.env.example` to `.env`.
2. Replace every placeholder with unique local values.
3. From the repository root, run:
```bash
docker compose up --build
```
- Angular SSR frontend: `http://localhost:4000`
- Backend API: `http://localhost:8080`
- MySQL: `localhost:3306`

Stop services with `docker compose down`. MySQL data is persisted in a named volume. `docker compose down -v` deletes that volume and its data.

**Deployment note:** `pg-management-frontend/src/app/core/services/api-config.ts` currently points to `http://localhost:8080/api/v1`. This is suitable for a local browser demo with the ports above, but must be changed to the correct HTTPS API URL for remote deployment. Configure CORS for the deployed frontend origin.

## API documentation
If enabled, Swagger UI may be available at `http://localhost:8080/swagger-ui/index.html`.

## Known limitations and deployment checklist
- `pg-management-backend/INTEGRATION-NOTES.md` documents room CRUD compatibility, tenant update/delete, and profile integration as requiring further work or verification.
- This archive preparation did not run the application, compile the backend, or run tests. Verify builds and important user journeys before deployment.
- Replace demo/local settings with secure production secrets and managed database credentials.
- Configure HTTPS, production CORS, backups, monitoring, log handling, and database migrations before real customer use.
- Verify SMS OTP provider configuration if SMS is enabled.
- Do not commit uploads containing private tenant documents or personal information.

## Contributing
Use a branch for each change and open a pull request for review. Never commit generated build folders, credentials, or production data.

## License
No license has been selected. Add a license after deciding how others may use and distribute this project.

## CI/CD Verification Test

Testing the GitHub Pull Request and CI workflow.
No application business logic has been changed.