#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

ENV_FILE="${SCRIPT_DIR}/.env"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
REGISTRY_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.registry.yml"

fail() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

info() {
  printf '[RUNNING] %s\n' "$*"
}

success() {
  printf '[DONE] %s\n' "$*"
}

check_runtime() {
  command -v docker >/dev/null 2>&1 || fail "Docker was not found."
  docker compose version >/dev/null 2>&1 || fail "Docker Compose Plugin is unavailable."
  [[ -f "${ENV_FILE}" ]] || fail "deploy/.env was not found."
  [[ -f "${COMPOSE_FILE}" ]] || fail "docker-compose.yml was not found."
  [[ -f "${REGISTRY_COMPOSE_FILE}" ]] || fail "docker-compose.registry.yml was not found."
}

compose() {
  docker compose \
    --env-file "${ENV_FILE}" \
    -f "${COMPOSE_FILE}" \
    -f "${REGISTRY_COMPOSE_FILE}" \
    "$@"
}

wait_healthy() {
  local service="$1"
  local timeout_seconds="${2:-180}"
  local deadline=$((SECONDS + timeout_seconds))
  local container_id
  local status

  container_id="$(compose ps -q "${service}")"
  [[ -n "${container_id}" ]] || return 1

  while (( SECONDS < deadline )); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}" 2>/dev/null || true)"
    case "${status}" in
      healthy|running) return 0 ;;
      unhealthy|exited|dead) return 1 ;;
    esac
    sleep 2
  done

  return 1
}

start_all() {
  info "Pulling fixed PC latest images"
  compose pull backend frontend

  info "Starting MySQL and Redis"
  compose up -d mysql redis
  wait_healthy mysql 300 || fail "MySQL did not become healthy."
  wait_healthy redis 180 || fail "Redis did not become healthy."

  info "Starting backend and frontend without building source code"
  compose up -d --no-build backend frontend
  wait_healthy backend 300 || fail "Backend did not become healthy."
  wait_healthy frontend 180 || fail "Frontend did not become healthy."

  success "All services are running"
  compose ps
}

update_all() {
  exec bash "${SCRIPT_DIR}/update-images.sh"
}

restart_all() {
  compose restart mysql redis backend frontend
  wait_healthy backend 300 || fail "Backend did not become healthy after restart."
  wait_healthy frontend 180 || fail "Frontend did not become healthy after restart."
  success "All services restarted"
  compose ps
}

usage() {
  cat <<'EOF'
Travel PC deployment tool

Usage:
  bash deploy.sh start          First deployment or start all services
  bash deploy.sh update         Back up the database, pull latest images, and update
  bash deploy.sh status         Show service status
  bash deploy.sh logs           Follow backend logs
  bash deploy.sh logs frontend  Follow frontend logs
  bash deploy.sh restart        Restart all services
  bash deploy.sh help           Show this help
EOF
}

check_runtime

case "${1:-start}" in
  start|deploy)
    start_all
    ;;
  update)
    update_all
    ;;
  status)
    compose ps
    ;;
  logs)
    compose logs -f --tail=200 "${2:-backend}"
    ;;
  restart)
    restart_all
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    usage
    fail "Unknown command: $1"
    ;;
esac
