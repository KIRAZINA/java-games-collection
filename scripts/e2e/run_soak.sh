#!/usr/bin/env bash
# End-to-end multiplayer soak: drives concurrent two-player rooms through the
# real running server over HTTP and asserts phase lifecycles, teardown, and
# leak-freedom.
#
# Usage:
#   bash scripts/e2e/run_soak.sh                 # 30s load, "SOAK OK"
#   bash scripts/e2e/run_soak.sh -d 60           # 60s load
#   bash scripts/e2e/run_soak.sh --chaos         # + SIGKILL/restart + double-ready
#   PORT=9090 BASE_URL=http://localhost:9090 bash scripts/e2e/run_soak.sh
#
# Exit 0 + "SOAK OK" (or "SOAK OK (chaos)") only when every player passed,
# the double-ready scenario passed (chaos mode), and no soak-* room leaked.
# Any PLAYER FAIL / stuck room / leaked room / failed scenario exits non-zero.

set -u

E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${PORT:-8080}"
BASE_URL="${BASE_URL:-http://localhost:$PORT}"
export PORT BASE_URL

DURATION=30
CHAOS=0
while [ $# -gt 0 ]; do
  case "$1" in
    -d) DURATION="${2:?-d needs seconds}"; shift 2 ;;
    --chaos) CHAOS=1; shift ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

WORK="${E2E_TMP:-${TMPDIR:-/tmp}}/e2e-soak-$$"
mkdir -p "$WORK"
CHAOS_FLAG="$WORK/.chaos-flag"
export E2E_CHAOS_FLAG="$CHAOS_FLAG"
[ "$CHAOS" = 1 ] || unset E2E_CHAOS_FLAG

TMP="$(mktemp)"
PLAYER_PIDS=()
PLAYER_LOGS=()
WAVES=0
FIRE_CHAOS_AT=$((DURATION / 2))
CHAOS_FIRED=0

cleanup() {
  rm -f "$TMP"
  bash "$E2E_DIR/stop_server.sh" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT

req_get() { # -> STATUS, BODY
  STATUS="$(curl -sS --max-time 10 -o "$TMP" -w '%{http_code}' "$BASE_URL$1" 2>/dev/null)" || STATUS="curl-error"
  BODY="$(cat "$TMP" 2>/dev/null || true)"
}

die() {
  echo "SOAK FAILED: $1" >&2
  [ -n "${2:-}" ] && { echo "--- $1 body ---" >&2; echo "$2" >&2; }
  exit 1
}

# --- start server ------------------------------------------------------------
START_OUT="$(bash "$E2E_DIR/start_server.sh" 2>&1)" || {
  echo "$START_OUT" >&2
  die "start_server.sh failed"
}
echo "$START_OUT"

# --- double-ready + race scenario (chaos mode, after restart) ---------------
double_ready_scenario() {
  echo "=== double-ready scenario ==="
  local pair="dr" wt wr out="$WORK/dr.out"
  STATUS="$(curl -sS --max-time 10 -o "$TMP" -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d "{\"roomName\":\"soak-$pair\",\"gameType\":\"MINESWEEPER\",\"settings\":{\"gameType\":\"MINESWEEPER\",\"settings\":{\"rows\":9,\"cols\":9,\"mines\":10},\"passwordProtected\":false,\"passwordHash\":null,\"allowBots\":false,\"maxPlayers\":2,\"timeLimitSeconds\":8,\"isSinglePlayer\":false},\"ownerId\":\"soak-$pair-own\",\"ownerName\":\"Dr Own\"}" \
    "$BASE_URL/api/rooms" 2>/dev/null)" || true
  [ "$STATUS" = 201 ] || die "double-ready: create room" "$(cat "$TMP")"
  local room token
  room="$(sed -n 's/.*"roomId":"\([^"]*\)".*/\1/p' "$TMP" | head -n 1)"
  token="$(sed -n 's/.*"playerToken":"\([^"]*\)".*/\1/p' "$TMP" | head -n 1)"
  STATUS="$(curl -sS --max-time 10 -o "$TMP" -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d '{"playerId":"soak-dr-gst","playerName":"Dr Gst"}' "$BASE_URL/api/rooms/$room/join" 2>/dev/null)" || true
  [ "$STATUS" = 200 ] || die "double-ready: join" "$(cat "$TMP")"
  local gtoken
  gtoken="$(sed -n 's/.*"playerToken":"\([^"]*\)".*/\1/p' "$TMP" | head -n 1)"

  # both readys fired concurrently: serialized server-side, both must be 200
  curl -sS --max-time 10 -o "$WORK/dr_r1" -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d '{"playerId":"soak-dr-own"}' -H "X-Player-Token: $token" "$BASE_URL/api/rooms/$room/ready" > "$WORK/dr_s1" 2>/dev/null &
  local p1=$!
  curl -sS --max-time 10 -o "$WORK/dr_r2" -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d '{"playerId":"soak-dr-gst"}' -H "X-Player-Token: $gtoken" "$BASE_URL/api/rooms/$room/ready" > "$WORK/dr_s2" 2>/dev/null &
  local p2=$!
  wait "$p1"; wait "$p2"
  local s1 s2
  s1="$(cat "$WORK/dr_s1")"; s2="$(cat "$WORK/dr_s2")"
  echo "  concurrent ready: owner=$s1 guest=$s2 (expect 200/200)"
  { [ "$s1" = 200 ] && [ "$s2" = 200 ]; } || die "double-ready: concurrent ready not both 200" "owner=$s1 guest=$s2 body1=$(cat "$WORK/dr_r1") body2=$(cat "$WORK/dr_r2")"

  # wait until the room has left LOBBY, then a third (late) ready must be 409
  local i=0 st=""
  while [ "$i" -lt 15 ]; do
    req_get "/api/rooms/$room/state"
    st="$(printf '%s' "$BODY" | sed -n 's/.*"state":"\([A-Z_]*\)".*/\1/p' | head -n 1)"
    [ "$st" != LOBBY ] && break
    i=$((i + 1)); sleep 1
  done
  [ "$st" != LOBBY ] || die "double-ready: room stuck in LOBBY" "$BODY"
  echo "  phase after readys: $st"
  STATUS="$(curl -sS --max-time 10 -o "$TMP" -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d '{"playerId":"soak-dr-own"}' -H "X-Player-Token: $token" "$BASE_URL/api/rooms/$room/ready" 2>/dev/null)" || true
  echo "  late duplicate ready: $STATUS (expect 409)"
  [ "$STATUS" = 409 ] || die "double-ready: late ready expected 409" "$(cat "$TMP")"

  # room must still settle (no stuck room after the duplicate/race traffic)
  i=0
  while [ "$i" -lt 30 ]; do
    req_get "/api/rooms/$room/state"
    case "$BODY" in *'"state":"GAME_OVER"'*) break ;; esac
    i=$((i + 1)); sleep 1
  done
  case "$BODY" in *'"state":"GAME_OVER"'*) ;; *) die "double-ready: room did not reach GAME_OVER (stuck room)" "$BODY" ;; esac

  STATUS="$(curl -sS --max-time 10 -o "$TMP" -w '%{http_code}' -X DELETE -H 'Content-Type: application/json' \
    -d '{"playerId":"soak-dr-gst"}' -H "X-Player-Token: $gtoken" "$BASE_URL/api/rooms/$room/leave" 2>/dev/null)" || true
  [ "$STATUS" = 204 ] || die "double-ready: guest leave" "$(cat "$TMP")"
  STATUS="$(curl -sS --max-time 10 -o "$TMP" -w '%{http_code}' -X DELETE -H 'Content-Type: application/json' \
    -d '{"requesterId":"soak-dr-own"}' -H "X-Player-Token: $token" "$BASE_URL/api/rooms/$room" 2>/dev/null)" || true
  [ "$STATUS" = 204 ] || die "double-ready: owner delete" "$(cat "$TMP")"
  echo "=== double-ready scenario OK ==="
}

fire_chaos() {
  local pid
  pid="$(cat "${E2E_PID_FILE:-${TMPDIR:-/tmp}/e2e-games-server.pid}" 2>/dev/null || true)"
  [ -n "$pid" ] || die "chaos: no server PID file"
  echo "=== chaos: SIGKILL pid=$pid ==="
  kill -9 "$pid" 2>/dev/null || true
  # confirm the process is truly gone (taskkill fallback for msys/native pid)
  sleep 1
  if kill -0 "$pid" 2>/dev/null; then
    taskkill //F //PID "$pid" >/dev/null 2>&1 || true
    sleep 1
  fi
  local i=0 up=1
  while [ "$i" -lt 10 ]; do
    curl -fsS --max-time 2 "$BASE_URL/actuator/health" >/dev/null 2>&1 || { up=0; break; }
    i=$((i + 1)); sleep 0.5
  done
  [ "$up" = 0 ] || die "chaos: server still healthy after SIGKILL"
  echo "  server confirmed down (health unreachable)"
  touch "$CHAOS_FLAG"
  local out
  out="$(bash "$E2E_DIR/start_server.sh" 2>&1)" || { echo "$out" >&2; die "chaos: restart failed"; }
  echo "$out"
  double_ready_scenario
}

# --- load loop ---------------------------------------------------------------
spawn_pair() {
  local wave="$1" game limit pair
  case $((wave % 3)) in
    0) game=MINESWEEPER;        limit=8 ;;
    1) game=TWENTY_FORTY_EIGHT; limit=8 ;;
    2) game=BLACKJACK;          limit=0 ;;
  esac
  pair="w$wave"
  local own_out="$WORK/$pair.own.out" gst_out="$WORK/$pair.gst.out"
  bash "$E2E_DIR/player.sh" owner "$pair" "$game" "$limit" "$WORK" > "$own_out" 2>&1 &
  PLAYER_PIDS+=($!); PLAYER_LOGS+=("$own_out")
  bash "$E2E_DIR/player.sh" guest "$pair" "$game" "$WORK" > "$gst_out" 2>&1 &
  PLAYER_PIDS+=($!); PLAYER_LOGS+=("$gst_out")
  echo "wave $wave: pair=$pair game=$game limit=${limit}s"
}

END_AT=$((SECONDS + DURATION))
while [ "$SECONDS" -lt "$END_AT" ]; do
  WAVES=$((WAVES + 1))
  spawn_pair "$WAVES"
  if [ "$CHAOS" = 1 ] && [ "$CHAOS_FIRED" = 0 ] && [ "$SECONDS" -ge "$FIRE_CHAOS_AT" ]; then
    CHAOS_FIRED=1
    fire_chaos
  fi
  sleep 3
done
if [ "$CHAOS" = 1 ] && [ "$CHAOS_FIRED" = 0 ]; then
  CHAOS_FIRED=1
  fire_chaos
fi
[ "$WAVES" -ge 1 ] || die "no waves spawned (duration too small?)"

# --- drain -------------------------------------------------------------------
echo "=== draining ${#PLAYER_PIDS[@]} player processes ==="
OK=0; FAILED=0; INTERRUPTED=0; FAILED_LOGS=""
idx=0
for pid in "${PLAYER_PIDS[@]}"; do
  log="${PLAYER_LOGS[$idx]}"; idx=$((idx + 1))
  if wait "$pid"; then rc=0; else rc=$?; fi
  if [ "$rc" -ne 0 ]; then
    FAILED=$((FAILED + 1))
    FAILED_LOGS="$FAILED_LOGS
--- $log (rc=$rc) ---
$(cat "$log")"
  else
    case "$(cat "$log")" in
      *"PLAYER INTERRUPTED"*) INTERRUPTED=$((INTERRUPTED + 1)) ;;
      *) OK=$((OK + 1)) ;;
    esac
  fi
done
if [ "$FAILED" -gt 0 ]; then
  echo "players failed: $FAILED (ok=$OK interrupted=$INTERRUPTED)" >&2
  echo "$FAILED_LOGS" >&2
  die "one or more players failed"
fi

# --- leak check -------------------------------------------------------------
sleep 1
req_get /api/rooms
[ "$STATUS" = 200 ] || die "final room list" "$BODY"
case "$BODY" in
  *'"roomName":"soak-'*)
    leaked="$(printf '%s' "$BODY" | grep -o '"roomName":"soak-[^"]*"' | tr '\n' ' ')"
    die "leaked soak rooms remain: $leaked"
    ;;
esac

echo "=== soak summary ==="
echo "  duration=${DURATION}s chaos=$CHAOS waves=$WAVES players=$((${#PLAYER_PIDS[@]})) ok=$OK interrupted=$INTERRUPTED failed=$FAILED"
echo "  final room list: no soak-* rooms (leak check clean)"
bash "$E2E_DIR/stop_server.sh" || die "stop_server failed"

if [ "$CHAOS" = 1 ]; then
  echo "SOAK OK (chaos)"
else
  echo "SOAK OK"
fi
exit 0
