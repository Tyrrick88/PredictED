# predictED API

Spring Boot backend for the predictED academic revision platform.

## Run locally

```bash
cd backend
DEBUG=false mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

On this workstation, the system Java path is a runtime-only install. If Maven says `release version 21 not supported`, run with the full JDK:

```bash
JAVA_HOME=/home/tyrrick-dev/.jdk/jdk-21.0.8 \
PATH=/home/tyrrick-dev/.jdk/jdk-21.0.8/bin:$PATH \
DEBUG=false \
mvn spring-boot:run
```

## Seed users

| Role | Email | Password |
| --- | --- | --- |
| Student | `alex@predicted.test` | `password` |
| Admin | `admin@predicted.test` | `admin123` |

## First request

```bash
TOKEN=$(curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alex@predicted.test","password":"password"}' \
  | jq -r .token)

curl http://localhost:8080/api/dashboard/overview \
  -H "Authorization: Bearer $TOKEN"
```

## MVP endpoints

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `GET /api/profile`
- `PUT /api/profile`
- `PUT /api/profile/courses`
- `GET /api/dashboard/overview`
- `GET /api/predictions/courses`
- `GET /api/predictions/{courseId}`
- `POST /api/predictions/{courseId}/simulate`
- `POST /api/predictions/{courseId}/mock`
- `GET /api/planner/today?focusHours=3`
- `POST /api/planner/tasks/{taskId}/complete`
- `POST /api/tutor/messages`
- `GET /api/flashcards/due`
- `POST /api/flashcards/{cardId}/review`
- `GET /api/feed`
- `POST /api/feed`
- `GET /api/marketplace/notes`
- `POST /api/payments/mpesa/stk-push`
- `GET /api/admin/moderation` with admin token

## Database migrations

Flyway owns the schema. Hibernate is set to `validate`, so startup fails if the database schema does not match the JPA models.

- Migrations live in `src/main/resources/db/migration`.
- Local H2 runs migrations automatically on startup.
- Existing local H2 files are baselined automatically, then the legacy-safe V2 enrollment migration runs.
- For a totally fresh local database, delete the ignored `backend/data` directory and start the API again.

## Local Postgres stack

From the repo root, start PostgreSQL and Redis:

```bash
docker compose up -d predicted-postgres predicted-redis
```

Then run the API with the Postgres profile:

```bash
set -a
source backend/env/postgres.example.env
set +a

cd backend
JAVA_HOME=/home/tyrrick-dev/.jdk/jdk-21.0.8 \
PATH=/home/tyrrick-dev/.jdk/jdk-21.0.8/bin:$PATH \
DEBUG=false \
mvn spring-boot:run
```

Flyway will create the schema, seed data will load, and the API will still start on `http://localhost:8080`.

## Production Postgres profile

The `postgres` profile is ready for environment-backed PostgreSQL, Redis, JWT configuration, and Flyway migrations:

```bash
SPRING_PROFILES_ACTIVE=postgres \
DATABASE_URL=jdbc:postgresql://localhost:5432/predicted \
DATABASE_USERNAME=predicted \
DATABASE_PASSWORD='replace-with-production-db-password' \
JWT_SECRET='replace-with-a-long-production-secret' \
mvn spring-boot:run
```

Production defaults:

- `JPA_DDL_AUTO=validate`
- `FLYWAY_ENABLED=true`
- `FLYWAY_BASELINE_ON_MIGRATE=false`

For a new production database, leave baseline disabled so Flyway creates the full schema from `V1__initial_schema.sql`. Only set `FLYWAY_BASELINE_ON_MIGRATE=true` when connecting to an existing schema you have already inspected.
