#!/usr/bin/env bash
# Khởi động lại toàn bộ môi trường dev bằng một lệnh: Postgres (Docker), backend, frontend.
#
#   ./scripts/dev.sh          tắt bản đang chạy (nếu có), bật lại cả 3, hiện log. Ctrl+C để tắt hết.
#   ./scripts/dev.sh --bg     như trên nhưng chạy nền, trả lại terminal. Log ở .dev-logs/
#   ./scripts/dev.sh stop     tắt backend và frontend (Postgres vẫn chạy để giữ dữ liệu)
#
# Yêu cầu: Docker, Java 21, Maven, Node 20+.
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS="$ROOT/.dev-logs"
BE_PORT=8080
FE_PORT=3000
mkdir -p "$LOGS"

c_info=$'\033[36m'; c_ok=$'\033[32m'; c_err=$'\033[31m'; c_off=$'\033[0m'
log()  { printf '%s[dev]%s %s\n' "$c_info" "$c_off" "$*"; }
ok()   { printf '%s[dev]%s %s\n' "$c_ok" "$c_off" "$*"; }
fail() { printf '%s[dev] %s%s\n' "$c_err" "$*" "$c_off" >&2; exit 1; }

port_pids() {
  if command -v lsof >/dev/null; then lsof -t -iTCP:"$1" -sTCP:LISTEN 2>/dev/null
  elif command -v fuser >/dev/null; then fuser "$1"/tcp 2>/dev/null
  fi
}

stop_one() { # $1 = backend|frontend, $2 = cổng
  local pidfile="$LOGS/$1.pid"
  if [ -f "$pidfile" ]; then
    local pid; pid="$(cat "$pidfile")"
    if kill -0 "$pid" 2>/dev/null; then
      log "Tắt $1 cũ (PID $pid)"
      kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
      for _ in $(seq 1 20); do kill -0 "$pid" 2>/dev/null || break; sleep 0.5; done
      kill -KILL -- "-$pid" 2>/dev/null
    fi
    rm -f "$pidfile"
  fi
  # Còn tiến trình khác giữ cổng (ví dụ chạy tay ở terminal khác) thì tắt nốt
  local left; left="$(port_pids "$2" | tr '\n' ' ')"
  if [ -n "${left// /}" ]; then
    log "Cổng $2 đang bị chiếm (PID $left), tắt để chạy lại $1"
    kill $left 2>/dev/null; sleep 2; kill -9 $left 2>/dev/null
  fi
}

stop_all() { stop_one frontend "$FE_PORT"; stop_one backend "$BE_PORT"; }

wait_http() { # $1 = tên, $2 = url, $3 = pid, $4 = số giây tối đa
  local i
  for i in $(seq 1 "$4"); do
    if curl -fsS -o /dev/null "$2" 2>/dev/null; then return 0; fi
    if ! kill -0 "$3" 2>/dev/null; then
      tail -n 40 "$LOGS/$1.log" >&2
      fail "$1 dừng khi đang khởi động. Xem log đầy đủ: .dev-logs/$1.log"
    fi
    sleep 1
  done
  tail -n 40 "$LOGS/$1.log" >&2
  fail "$1 chưa sẵn sàng sau $4 giây. Xem log: .dev-logs/$1.log"
}

check_tools() {
  local t
  for t in docker mvn npm curl setsid; do
    command -v "$t" >/dev/null || fail "Thiếu lệnh '$t'. Cài đặt rồi chạy lại."
  done
  local v; v="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
  [ -n "$v" ] && [ "$v" -ge 21 ] 2>/dev/null \
    || fail "Cần Java 21 trở lên (đang là ${v:-không tìm thấy}). Đặt JAVA_HOME tới JDK 21 rồi chạy lại."
  docker info >/dev/null 2>&1 || fail "Docker chưa chạy hoặc user chưa có quyền dùng Docker."
}

start_db() {
  log "Bật Postgres"
  (cd "$ROOT" && docker compose up -d postgres >/dev/null) || fail "Không bật được Postgres (docker compose up)."
  local i s
  for i in $(seq 1 60); do
    s="$(docker inspect -f '{{.State.Health.Status}}' aar-postgres 2>/dev/null)"
    [ "$s" = "healthy" ] && { ok "Postgres sẵn sàng"; return; }
    sleep 1
  done
  fail "Postgres chưa sẵn sàng sau 60 giây. Xem: docker compose logs postgres"
}

start_backend() {
  log "Bật backend (profile dev, lần đầu có thể mất vài phút)..."
  (cd "$ROOT/backend" && exec setsid mvn -q spring-boot:run -Dspring-boot.run.profiles=dev) > "$LOGS/backend.log" 2>&1 &
  echo $! > "$LOGS/backend.pid"
  wait_http backend "http://localhost:$BE_PORT/actuator/health" "$!" 300
  ok "Backend sẵn sàng: http://localhost:$BE_PORT  (Swagger: /swagger-ui.html)"
}

start_frontend() {
  cd "$ROOT/frontend" || exit 1
  [ -f .env.local ] || { cp .env.example .env.local; log "Đã tạo frontend/.env.local từ .env.example"; }
  if [ ! -d node_modules ] || [ package-lock.json -nt node_modules/.package-lock.json ]; then
    log "Cài thư viện frontend (npm ci)..."
    npm ci --no-audit --no-fund > "$LOGS/npm-ci.log" 2>&1 || { tail -n 30 "$LOGS/npm-ci.log" >&2; fail "npm ci lỗi"; }
  fi
  log "Bật frontend"
  (exec setsid npm run dev -- --port "$FE_PORT") > "$LOGS/frontend.log" 2>&1 &
  echo $! > "$LOGS/frontend.pid"
  cd "$ROOT" || exit 1
  wait_http frontend "http://localhost:$FE_PORT" "$!" 120
  ok "Frontend sẵn sàng: http://localhost:$FE_PORT"
}

case "${1:-}" in
  stop) stop_all; ok "Đã tắt backend và frontend. Tắt cả Postgres: docker compose stop"; exit 0 ;;
  ""|--bg) ;;
  *) fail "Không hiểu tham số '$1'. Dùng: dev.sh [--bg|stop]" ;;
esac

check_tools
# Ctrl+C lúc đang khởi động cũng tắt sạch, không để tiến trình chạy ngầm
trap 'echo; stop_all; exit 130' INT TERM
stop_all
start_db
start_backend
start_frontend

echo
ok "Xong. Mở http://localhost:$FE_PORT  · tài khoản mẫu: manager@aar.local / staff1@aar.local, mật khẩu Password@123"

if [ "${1:-}" = "--bg" ]; then
  log "Đang chạy nền. Xem log: tail -f .dev-logs/backend.log .dev-logs/frontend.log  · Tắt: ./scripts/dev.sh stop"
  exit 0
fi

log "Đang hiện log của cả hai. Ctrl+C để tắt backend và frontend."
trap 'echo; stop_all; ok "Đã tắt."; exit 0' INT TERM
tail -n 0 -F "$LOGS/backend.log" "$LOGS/frontend.log" &
TAIL=$!
# Thoát khi một trong hai tự dừng (ví dụ lỗi), để không treo màn hình log
while kill -0 "$(cat "$LOGS/backend.pid" 2>/dev/null)" 2>/dev/null \
   && kill -0 "$(cat "$LOGS/frontend.pid" 2>/dev/null)" 2>/dev/null; do
  sleep 2
done
kill "$TAIL" 2>/dev/null
stop_all
fail "Backend hoặc frontend đã dừng. Xem log trong .dev-logs/"
