#!/usr/bin/env bash
# Stops the games-backend server started by start_server.sh.
#
# Usage:
#   bash scripts/e2e/stop_server.sh
#
# Sequence: SIGTERM (server.shutdown=graceful drains Tomcat), wait for the
# health endpoint to go down; if still up after 10s escalate to taskkill /F,
# then verify. Idempotent: no PID file and no healthy server = exit 0.

set -u

E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${PORT:-8080}"
BASE_URL="${BASE_URL:-http://localhost:$PORT}"
PID_FILE="${E2E_PID_FILE:-${TMPDIR:-/tmp}/e2e-games-server.pid}"

server_healthy() {
  curl -fsS --max-time 3 "$BASE_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'
}

PID=""
[ -f "$PID_FILE" ] && PID="$(cat "$PID_FILE" 2>/dev/null || true)"

if [ -z "$PID" ]; then
  if server_healthy; then
    echo "FATAL: health endpoint is up but no PID file found ($PID_FILE); not killing an unknown process" >&2
    exit 1
  fi
  echo "server not running"
  exit 0
fi

# The PID file holds the WINDOWS pid (see start_server.sh): msys kill is not
# reliable across process namespaces, so signal via taskkill ( // = / after
# msys argument conversion ).
taskkill //PID "$PID" >/dev/null 2>&1 || true
for _ in $(seq 1 10); do
  server_healthy || break
  sleep 1
done

if server_healthy; then
  echo "graceful stop did not finish; escalating to taskkill /F pid=$PID"
  taskkill //F //PID "$PID" >/dev/null 2>&1 || true
  for _ in $(seq 1 5); do
    server_healthy || break
    sleep 1
  done
fi

if server_healthy; then
  echo "FATAL: server still healthy after stop attempts; log tail:" >&2
  tail -n 20 "${E2E_LOG_FILE:-${TMPDIR:-/tmp}/e2e-games-server.log}" >&2 || true
  exit 1
fi

rm -f "$PID_FILE"
echo "server stopped pid=$PID"
exit 0
