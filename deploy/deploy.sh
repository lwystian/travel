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

check_runtime() {
  command -v docker >/dev/null 2>&1 || fail "没有找到 Docker。"
  docker compose version >/dev/null 2>&1 || fail "Docker Compose Plugin 不可用。"
  [[ -f "${ENV_FILE}" ]] || fail "没有找到 deploy/.env。"
  [[ -f "${COMPOSE_FILE}" ]] || fail "没有找到 docker-compose.yml。"
  [[ -f "${REGISTRY_COMPOSE_FILE}" ]] || fail "没有找到 docker-compose.registry.yml。"
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
  info "拉取 PC 前后端固定 latest 镜像"
  compose pull backend frontend

  info "启动 MySQL 和 Redis"
  compose up -d mysql redis
  wait_healthy mysql 300 || fail "MySQL 未通过健康检查。"
  wait_healthy redis 180 || fail "Redis 未通过健康检查。"

  info "直接启动前后端镜像，不在服务器构建源码"
  compose up -d --no-build backend frontend
  wait_healthy backend 300 || fail "后端未通过健康检查。"
  wait_healthy frontend 180 || fail "前端未通过健康检查。"

  success "全部服务已启动"
  compose ps
}

update_all() {
  exec bash "${SCRIPT_DIR}/update-images.sh"
}

restart_all() {
  compose restart mysql redis backend frontend
  wait_healthy backend 300 || fail "重启后端后健康检查失败。"
  wait_healthy frontend 180 || fail "重启前端后健康检查失败。"
  success "全部服务已重启"
  compose ps
}

usage() {
  cat <<'EOF'
Travel PC 部署工具

用法：
  ./deploy.sh start          首次部署或启动全部服务
  ./deploy.sh update         备份数据库、拉取最新镜像并更新
  ./deploy.sh status         查看服务状态
  ./deploy.sh logs           查看后端日志
  ./deploy.sh logs frontend  查看前端日志
  ./deploy.sh restart        重启全部服务
  ./deploy.sh help           查看帮助
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
    fail "未知命令：$1"
    ;;
esac
