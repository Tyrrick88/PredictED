# PredictED Deployment

This repo is set up for a split deployment:

- Frontend: Vercel, project root `frontend`
- Backend: Railway, project root repo root using `railway.json` and `Dockerfile`

Payments are intentionally skipped for this deployment. Marketplace files can be uploaded and downloaded, but paid checkout is not active.

## Railway Backend

Create a Railway project from this GitHub repo and add a PostgreSQL database.

Open the backend service, not the PostgreSQL service, then go to Variables. If Railway offers variables from `.env.example`, import them there. You can also paste this block into the raw editor:

```bash
SPRING_PROFILES_ACTIVE=postgres
PORT=8080
PREDICTED_SECURITY_PRODUCTION=true
JWT_SECRET=replace-with-64-plus-random-characters
JWT_EXPIRATION_MINUTES=60
CORS_ALLOWED_ORIGIN_PATTERNS=https://predict-ed.vercel.app
PREDICTED_SEED_DEMO_USERS_ENABLED=false
PREDICTED_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
PREDICTED_BOOTSTRAP_ADMIN_PASSWORD=replace-with-a-strong-admin-password-14-plus-chars
PREDICTED_BOOTSTRAP_ADMIN_NAME=PredictED Admin
RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_PER_MINUTE=8
RATE_LIMIT_UPLOADS_PER_MINUTE=12
RATE_LIMIT_AI_PER_MINUTE=30
RATE_LIMIT_API_PER_MINUTE=240
AI_PROVIDER=fallback
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4o
UPLOAD_ROOT=/data/uploads
UPLOAD_MAX_FILE_SIZE=20MB
UPLOAD_MAX_REQUEST_SIZE=25MB
UPLOAD_MAX_FILE_BYTES=20971520
```

For Postgres, either let the Railway Postgres plugin provide `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD`, or set these explicitly:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://host:port/database
SPRING_DATASOURCE_USERNAME=postgres-user
SPRING_DATASOURCE_PASSWORD=postgres-password
```

The root `.env.example` uses Railway variable references like `${{Postgres.PGHOST}}`. If your database service has a different name, update `Postgres` in those references to match the actual database service name.

If using uploads in production, add a Railway volume mounted at `/data`.

## Vercel Frontend

Create a Vercel project from this repo with:

- Root Directory: `frontend`
- Build Command: `npm run build`
- Output Directory: `dist`

The frontend build defaults to the live Railway backend:

```bash
https://predicted-production-0574.up.railway.app
```

If the Railway backend domain changes later, set this Vercel environment variable to override the built-in default:

```bash
PREDICTED_API_BASE=https://your-new-backend.up.railway.app
PREDICTED_DEMO_LOGIN_ENABLED=false
```

Redeploy Vercel after changing the variable.

## Smoke Test

1. Open the Vercel URL.
2. Register a student account with a strong password.
3. Confirm dashboard, courses, tutor fallback, marketplace uploads, and downloads work.
4. Sign in with the `PREDICTED_BOOTSTRAP_ADMIN_EMAIL` account to check moderation and admin catalog tools.

Production startup intentionally fails if CORS is wildcarded, the JWT secret is a placeholder or shorter than 64 characters, the H2 console is enabled, rate limiting is disabled, or demo users are enabled.
