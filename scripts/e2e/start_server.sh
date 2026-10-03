#!/usr/bin/env bash
# Boots games-backend from its executable jar and waits for /actuator/health.
#
# Usage:
#   bash scripts/e2e/start_server.sh            # PORT=8080 default
#   PORT=9090 bash scripts/e2e/start_server.sh
#   E2E_REBUILD=1 bash scripts/e2e/start_server.sh   # force mvn package first
#
# Idempotent: if a server started by this script is already alive (PID file +
# health check), prints "server already running" and exits 0.
# On success prints "server up" with pid and base url, exit 0.
# On failure dumps the tail of the server log, exit 1.

set -u

E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$E2E_DIR/../.." && pwd)"
PORT="${PORT:-8080}"
BASE_URL="${BASE_URL:-http://localhost:$PORT}"
PID_FILE="${E2E_PID_FILE:-${TMPDIR:-/tmp}/e2e-games-server.pid}"
LOG_FILE="${E2E_LOG_FILE:-${TMPDIR:-/tmp}/e2e-games-server.log}"

jar_path() {
  ls "$ROOT_DIR"/games-backend/target/games-backend-*.jar 2>/dev/null | grep -v '\.original$' | head -n 1
}

server_healthy() {
  curl -fsS --max-time 3 "$BASE_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'
}

if [ -f "$PID_FILE" ] && server_healthy; then
  old_pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  echo "server already running pid=$old_pid base=$BASE_URL"
  exit 0
fi
rm -f "$PID_FILE"

JAR="$(jar_path)"
if [ -z "$JAR" ] || [ "${E2E_REBUILD:-0}" = "1" ]; then
  echo "building games-backend jar ..."
  (cd "$ROOT_DIR" && mvn -pl games-backend package -DskipTests -q) || {
    echo "FATAL: mvn package failed" >&2
    exit 1
  }
  JAR="$(jar_path)"
  [ -n "$JAR" ] || { echo "FATAL: jar not found after build" >&2; exit 1; }
fi

: > "$LOG_FILE"
PORT="$PORT" java -jar "$JAR" > "$LOG_FILE" 2>&1 &
SERVER_PID=$!
echo "$SERVER_PID" > "$PID_FILE"
echo "started java pid=$SERVER_PID jar=$(basename "$JAR") log=$LOG_FILE"

for _ in $(seq 1 90); do
  if server_healthy; then
    # Record the WINDOWS pid: msys $! can differ from the Windows process id
    # (kill/taskkill consumers need the Windows pid). netstat lists it for the
    # listening socket; fall back to the msys pid if parsing ever fails.
    WIN_PID="$(netstat -ano | grep LISTENING | grep ":$PORT " | head -n 1 | awk '{print $NF}')"
    if [ -n "$WIN_PID" ]; then
      echo "$WIN_PID" > "$PID_FILE"
      echo "server up pid=$WIN_PID (shell pid=$SERVER_PID) base=$BASE_URL"
    else
      echo "$SERVER_PID" > "$PID_FILE"
      echo "server up pid=$SERVER_PID (WARNING: netstat pid lookup failed) base=$BASE_URL"
    fi
    exit 0
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "FATAL: server process died during startup; log tail:" >&2
    tail -n 40 "$LOG_FILE" >&2
    rm -f "$PID_FILE"
    exit 1
  fi
  sleep 1
done

echo "FATAL: server did not become healthy within 90s; log tail:" >&2
tail -n 40 "$LOG_FILE" >&2
rm -f "$PID_FILE"
exit 1
