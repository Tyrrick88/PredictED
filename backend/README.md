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
- `GET /api/ai/status`
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
- `POST /api/marketplace/notes` multipart upload
- `GET /api/marketplace/notes/{noteId}/download`
- `POST /api/payments/mpesa/stk-push` mocked only; not used by the deploy frontend
- `GET /api/admin/moderation` with admin token

## Deployment status

Payments are skipped for the first hosted deployment. The backend still has a queued-payment stub for tests, but the frontend no longer starts M-Pesa checkout.

For split hosting:

- Deploy the backend to Railway from the repo root. `railway.json` and `nixpacks.toml` build the Spring Boot app from `backend`.
- Deploy the frontend to Vercel from the `frontend` root. Set `PREDICTED_API_BASE` in Vercel to the Railway public backend URL.
- Set `CORS_ALLOWED_ORIGIN_PATTERNS` in Railway to the Vercel domain.

## File uploads

Authenticated students can upload note packs into the marketplace:

```bash
curl http://localhost:8080/api/marketplace/notes \
  -H "Authorization: Bearer $TOKEN" \
  -F "title=Vector Clocks Revision Pack" \
  -F "courseId=distributed" \
  -F "priceKes=0" \
  -F "file=@/path/to/notes.pdf"
```

Uploaded files are stored under `UPLOAD_ROOT` with metadata in Postgres/H2. Defaults accept PDF, Word, PowerPoint, text, and image files up to 20MB. New uploads are listed as marketplace packs, queued for moderation, and can be downloaded with the returned `downloadUrl`.

## Real AI services

The tutor and mock-paper endpoints use a provider-backed AI service:

- `POST /api/tutor/messages`
- `POST /api/predictions/{courseId}/mock`

Default mode is deterministic fallback so local development and tests work without an API key. To use OpenAI:

```bash
AI_PROVIDER=openai \
OPENAI_API_KEY='sk-your-key' \
OPENAI_MODEL=gpt-5.5 \
mvn spring-boot:run
```

The implementation calls the OpenAI Responses API with Structured Outputs, so AI tutor replies and mock questions are parsed into typed API responses. Check the active mode with:

```bash
curl http://localhost:8080/api/ai/status \
  -H "Authorization: Bearer $TOKEN"
```

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

If Docker is not installed on Ubuntu yet, run this from your local terminal so sudo can securely ask for your password:

```bash
./scripts/install-docker-ubuntu.sh
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

The helper script wraps those steps:

```bash
./scripts/run-backend-postgres.sh
```

## Production Postgres profile

The `postgres` profile is ready for environment-backed PostgreSQL, Redis, JWT configuration, and Flyway migrations:

```bash
SPRING_PROFILES_ACTIVE=postgres \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/predicted \
SPRING_DATASOURCE_USERNAME=predicted \
SPRING_DATASOURCE_PASSWORD='replace-with-production-db-password' \
JWT_SECRET='replace-with-a-long-production-secret' \
CORS_ALLOWED_ORIGIN_PATTERNS='https://your-vercel-domain.vercel.app' \
mvn spring-boot:run
```

Production defaults:

- `JPA_DDL_AUTO=validate`
- `FLYWAY_ENABLED=true`
- `FLYWAY_BASELINE_ON_MIGRATE=false`

For a new production database, leave baseline disabled so Flyway creates the full schema from `V1__initial_schema.sql`. Only set `FLYWAY_BASELINE_ON_MIGRATE=true` when connecting to an existing schema you have already inspected.
