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
- `GET /api/auth/me`
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

## Production profile placeholder

The `postgres` profile is ready for environment-backed PostgreSQL, Redis, and JWT configuration:

```bash
SPRING_PROFILES_ACTIVE=postgres \
DATABASE_URL=jdbc:postgresql://localhost:5432/predicted \
DATABASE_USERNAME=predicted \
DATABASE_PASSWORD=predicted \
JWT_SECRET='replace-with-a-long-production-secret' \
mvn spring-boot:run
```
