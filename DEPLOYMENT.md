# PredictED Deployment

This repo is set up for a split deployment:

- Frontend: Vercel, project root `frontend`
- Backend: Railway, project root repo root using `railway.json` and `nixpacks.toml`

Payments are intentionally skipped for this deployment. Marketplace files can be uploaded and downloaded, but paid checkout is not active.

## Railway Backend

Create a Railway project from this GitHub repo and add a PostgreSQL database.

Set these backend variables:

```bash
SPRING_PROFILES_ACTIVE=postgres
PORT=8080
JWT_SECRET=replace-with-64-plus-random-characters
CORS_ALLOWED_ORIGIN_PATTERNS=https://predict-ed.vercel.app
AI_PROVIDER=fallback
OPENAI_API_KEY=
OPENAI_MODEL=gpt-5.5
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
```

Redeploy Vercel after changing the variable.

## Smoke Test

1. Open the Vercel URL.
2. Sign in with `alex@predicted.test` / `password`.
3. Confirm dashboard, courses, tutor fallback, marketplace uploads, downloads, and admin login work.
4. Sign in with `admin@predicted.test` / `admin123` to check moderation.
