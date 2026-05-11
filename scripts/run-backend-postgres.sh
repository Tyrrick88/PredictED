#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${REPO_ROOT}/backend"
ENV_FILE="${BACKEND_DIR}/env/postgres.example.env"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed. Run scripts/install-docker-ubuntu.sh first." >&2
  exit 1
fi

cd "$REPO_ROOT"
docker compose up -d predicted-postgres predicted-redis

echo "Waiting for PostgreSQL and Redis health checks..."
for service in predicted-postgres predicted-redis; do
  for _ in {1..60}; do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$service" 2>/dev/null || true)"
    if [[ "$status" == "healthy" ]]; then
      echo "$service is healthy."
      break
    fi
    sleep 2
  done
  status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$service" 2>/dev/null || true)"
  if [[ "$status" != "healthy" ]]; then
    echo "$service did not become healthy. Current status: $status" >&2
    docker compose ps
    exit 1
  fi
done

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if [[ -x "/home/tyrrick-dev/.jdk/jdk-21.0.8/bin/javac" ]]; then
  export JAVA_HOME="/home/tyrrick-dev/.jdk/jdk-21.0.8"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

cd "$BACKEND_DIR"
DEBUG=false mvn spring-boot:run
