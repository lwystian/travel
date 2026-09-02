#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

ENV_FILE="${SCRIPT_DIR}/.env"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
REGISTRY_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.registry.yml"

fail() {
  printf '[失败] %s\n' "$*" >&2
  exit 1
}

info() {
  printf '[进行中] %s\n' "$*"
}

success() {
  printf '[完成] %s\n' "$*"
}

command -v docker >/dev/null 2>&1 || fail "没有找到 Docker。"
docker compose version >/dev/null 2>&1 || fail "Docker Compose Plugin 不可用。"
[[ -f "${ENV_FILE}" ]] || fail "没有找到 deploy/.env。"
[[ -f "${COMPOSE_FILE}" ]] || fail "没有找到 docker-compose.yml。"
[[ -f "${REGISTRY_COMPOSE_FILE}" ]] || fail "没有找到 docker-compose.registry.yml。"

compose() {
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" -f "${REGISTRY_COMPOSE_FILE}" "$@"
}

container_image_id() {
  local service="$1"
  local container_id
  container_id="$(compose ps -q "${service}" 2>/dev/null || true)"
  [[ -n "${container_id}" ]] || return 0
  docker inspect --format '{{.Image}}' "${container_id}"
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

show_failure_details() {
  local service="$1"
  local container_id
  container_id="$(compose ps -q "${service}" 2>/dev/null || true)"
  printf '[诊断] %s 容器未通过健康检查，以下是最近日志：\n' "${service}" >&2
  if [[ -n "${container_id}" ]]; then
    docker inspect --format '容器状态={{.State.Status}}，健康状态={{if .State.Health}}{{.State.Health.Status}}{{else}}未配置{{end}}' "${container_id}" >&2 || true
  fi
  compose logs --no-color --tail=120 "${service}" >&2 || true
}

rollback() {
  local backend_image="$1"
  local frontend_image="$2"
  local backend_target="$3"
  local frontend_target="$4"
  printf '[回退] 新镜像启动失败，正在恢复更新前镜像。\n' >&2
  if [[ -n "${backend_image}" ]]; then docker tag "${backend_image}" "${backend_target}"; fi
  if [[ -n "${frontend_image}" ]]; then docker tag "${frontend_image}" "${frontend_target}"; fi
  compose up -d --no-build --no-deps --force-recreate backend frontend >/dev/null 2>&1 || true
}

compose ps -q mysql | grep -q . || fail "MySQL 未运行，已停止更新。"
compose ps -q backend | grep -q . || fail "后端未运行；首次部署请先按 README 启动基础服务。"
compose ps -q frontend | grep -q . || fail "前端未运行；首次部署请先按 README 启动基础服务。"

travel_data_dir="$(sed -n 's/^TRAVEL_DATA_DIR=//p' "${ENV_FILE}" | tail -n 1 | tr -d '\r')"
travel_data_dir="${travel_data_dir:-./travel-data}"
if [[ "${travel_data_dir}" != /* ]]; then
  travel_data_dir="${SCRIPT_DIR}/${travel_data_dir#./}"
fi
backup_dir="${travel_data_dir}/deploy-backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "${backup_dir}"
info "备份数据库到 ${backup_dir}"
compose exec -T mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --routines --triggers "$MYSQL_DATABASE"' \
  | gzip -c > "${backup_dir}/tourism_system.sql.gz"
gzip -t "${backup_dir}/tourism_system.sql.gz" || fail "数据库备份校验失败。"

old_backend_id="$(container_image_id backend)"
old_frontend_id="$(container_image_id frontend)"
image_registry="$(sed -n 's/^IMAGE_REGISTRY=//p' "${ENV_FILE}" | tail -n 1 | tr -d '\r')"
image_registry="${image_registry:-ccr.ccs.tencentyun.com/lwystian/travel}"
image_registry="${image_registry%/}"
backend_target="${image_registry}:pc-backend-latest"
frontend_target="${image_registry}:pc-frontend-latest"

if [[ -n "${old_backend_id}" ]]; then docker tag "${old_backend_id}" "${backend_target%-latest}-previous"; fi
if [[ -n "${old_frontend_id}" ]]; then docker tag "${old_frontend_id}" "${frontend_target%-latest}-previous"; fi

info "从镜像仓库拉取 PC 前后端 latest 镜像"
compose pull backend frontend

info "切换后端容器"
compose up -d --no-build --no-deps --force-recreate backend
if ! wait_healthy backend 300; then
  show_failure_details backend
  rollback "${old_backend_id}" "${old_frontend_id}" "${backend_target}" "${frontend_target}"
  fail "新后端未通过健康检查，已尝试恢复旧镜像。"
fi

info "切换前端容器"
compose up -d --no-build --no-deps --force-recreate frontend
if ! wait_healthy frontend 180; then
  show_failure_details frontend
  rollback "${old_backend_id}" "${old_frontend_id}" "${backend_target}" "${frontend_target}"
  fail "新前端未通过健康检查，已尝试恢复旧镜像。"
fi

success "更新完成，服务器未执行 Maven 或 npm 构建。"
compose ps backend frontend
