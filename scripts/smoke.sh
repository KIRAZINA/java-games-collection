#!/usr/bin/env bash
# 16-step API smoke test for games-backend.
#
# Usage:
#   bash scripts/smoke.sh                                  # http://localhost:8080
#   BASE_URL=https://api.example.com bash scripts/smoke.sh
#
# Prints "SMOKE OK" and exits 0 when every step passes.
# On the first failure prints the step number, script line, expected vs actual
# status and the response body, then exits non-zero.

set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

STEP=0
STATUS=""
BODY=""

die() {
  echo "" >&2
  echo "SMOKE FAILED at step $STEP (scripts/smoke.sh line ${BASH_LINENO[0]}): $1" >&2
  echo "  base url: $BASE_URL" >&2
  exit 1
}

begin_step() {
  STEP="$1"
  printf 'step %02d: %s %s ... ' "$STEP" "$2" "$3"
}

ok() {
  printf 'ok\n'
}

# request <method> <path> [json-body] [player-token]
# Sets STATUS (http code) and BODY (response payload).
request() {
  local method="$1" path="$2" json="${3-}" token="${4-}"
  local -a args=(-sS --max-time 15 -X "$method" -o "$TMP_BODY" -w '%{http_code}')
  if [ -n "$json" ]; then
    args+=(-H 'Content-Type: application/json' -d "$json")
  fi
  if [ -n "$token" ]; then
    args+=(-H "X-Player-Token: $token")
  fi
  if ! STATUS="$(curl "${args[@]}" "$BASE_URL$path")"; then
    die "curl failed for $method $path"
  fi
  BODY="$(cat "$TMP_BODY")"
}

expect_status() {
  [ "$STATUS" = "$1" ] || die "$2: expected HTTP $1, got $STATUS; body: $BODY"
}

expect_contains() {
  case "$BODY" in
    *"$1"*) ;;
    *) die "$2: body missing '$1'; body: $BODY" ;;
  esac
}

expect_empty_body() {
  [ -z "$BODY" ] || die "$1: expected empty body; body: $BODY"
}

extract() {
  printf '%s' "$BODY" | sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -n 1
}

require_field() {
  [ -n "$2" ] || die "$1: field '$3' missing; body: $BODY"
}

# --- step 1: health ---------------------------------------------------------
begin_step 1 GET /actuator/health
request GET /actuator/health
expect_status 200 "health check"
expect_contains '"status":"UP"' "health check"
ok

# --- step 2: blackjack session ---------------------------------------------
begin_step 2 POST /api/blackjack/sessions
request POST /api/blackjack/sessions '{"initialBalance":100,"difficulty":"BASIC"}'
expect_status 201 "create blackjack session"
BJ_SESSION="$(extract sessionId)"
require_field "create blackjack session" "$BJ_SESSION" sessionId
ok

# --- steps 3+4: round then bet ---------------------------------------------
# A natural blackjack (player or dealer) auto-settles the round at bet time,
# so replay the round/bet pair until the player actually gets a turn.
attempt=0
while :; do
  begin_step 3 POST /api/blackjack/sessions/$BJ_SESSION/rounds
  request POST "/api/blackjack/sessions/$BJ_SESSION/rounds" '{}'
  expect_status 200 "start round"
  expect_contains '"phase"' "start round"
  ok

  begin_step 4 POST /api/blackjack/sessions/$BJ_SESSION/bets
  request POST "/api/blackjack/sessions/$BJ_SESSION/bets" '{"amount":10}'
  expect_status 200 "place bet"
  expect_contains '"currentBet":10' "place bet"
  case "$BODY" in
    *'"phase":"PLAYER_TURN"'*)
      ok
      break
      ;;
    *)
      printf 'natural blackjack - replaying round/bet\n'
      attempt=$((attempt + 1))
      [ "$attempt" -ge 10 ] && die "place bet: no PLAYER_TURN after $attempt replays"
      ;;
  esac
done

# --- step 5: stand ----------------------------------------------------------
begin_step 5 POST /api/blackjack/sessions/$BJ_SESSION/stand
request POST "/api/blackjack/sessions/$BJ_SESSION/stand" '{}'
expect_status 200 "stand"
expect_contains '"phase":"ROUND_OVER"' "stand"
ok

# --- step 6: minesweeper session -------------------------------------------
begin_step 6 POST /api/minesweeper/sessions
request POST /api/minesweeper/sessions '{"rows":5,"cols":5,"mines":3}'
expect_status 201 "create minesweeper session"
MS_SESSION="$(extract sessionId)"
require_field "create minesweeper session" "$MS_SESSION" sessionId
ok

# --- step 7: open a cell ----------------------------------------------------
begin_step 7 POST /api/minesweeper/sessions/$MS_SESSION/open
request POST "/api/minesweeper/sessions/$MS_SESSION/open" '{"row":1,"col":1}'
expect_status 200 "open cell"
expect_contains '"firstClickDone":true' "open cell"
ok

# --- step 8: 2048 session ---------------------------------------------------
begin_step 8 POST /api/2048/sessions
request POST /api/2048/sessions
expect_status 201 "create 2048 session"
TF_SESSION="$(extract sessionId)"
require_field "create 2048 session" "$TF_SESSION" sessionId
ok

# --- step 9: move left ------------------------------------------------------
begin_step 9 POST /api/2048/sessions/$TF_SESSION/moves
request POST "/api/2048/sessions/$TF_SESSION/moves" '{"direction":"LEFT"}'
expect_status 200 "move left"
expect_contains '"movesMade"' "move left"
ok

# --- step 10: create room (multiplayer blackjack) --------------------------
begin_step 10 POST /api/rooms
request POST /api/rooms '{"roomName":"smoke-room","gameType":"BLACKJACK","settings":{"gameType":"BLACKJACK","settings":{"initialBalance":100,"difficulty":"BASIC"},"passwordProtected":false,"passwordHash":null,"allowBots":true,"maxPlayers":4,"timeLimitSeconds":0,"isSinglePlayer":false},"ownerId":"smoke-p1","ownerName":"Smoke P1"}'
expect_status 201 "create room"
ROOM_ID="$(extract roomId)"
OWNER_TOKEN="$(extract playerToken)"
require_field "create room" "$ROOM_ID" roomId
require_field "create room" "$OWNER_TOKEN" playerToken
expect_contains '"phase":"LOBBY"' "create room"
ok

# --- step 11: second player joins -----------------------------------------
begin_step 11 POST /api/rooms/$ROOM_ID/join
request POST "/api/rooms/$ROOM_ID/join" '{"playerId":"smoke-p2","playerName":"Smoke P2"}'
expect_status 200 "join room"
GUEST_TOKEN="$(extract playerToken)"
require_field "join room" "$GUEST_TOKEN" playerToken
ok

# --- step 12: owner readies -------------------------------------------------
begin_step 12 POST /api/rooms/$ROOM_ID/ready
request POST "/api/rooms/$ROOM_ID/ready" '{"playerId":"smoke-p1"}' "$OWNER_TOKEN"
expect_status 200 "owner ready"
ok

# --- step 13: second player readies ----------------------------------------
begin_step 13 POST /api/rooms/$ROOM_ID/ready
request POST "/api/rooms/$ROOM_ID/ready" '{"playerId":"smoke-p2"}' "$GUEST_TOKEN"
expect_status 200 "guest ready"
ok

# --- step 14: room state shows READY_CHECK or PLAYING ----------------------
begin_step 14 GET /api/rooms/$ROOM_ID/state
request GET "/api/rooms/$ROOM_ID/state"
expect_status 200 "room state"
case "$BODY" in
  *'"state":"READY_CHECK"'*|*'"state":"PLAYING"'*)
    ok
    ;;
  *)
    die "room state: expected phase READY_CHECK or PLAYING; body: $BODY"
    ;;
esac

# --- step 15: second player leaves ----------------------------------------
begin_step 15 DELETE /api/rooms/$ROOM_ID/leave
request DELETE "/api/rooms/$ROOM_ID/leave" '{"playerId":"smoke-p2"}' "$GUEST_TOKEN"
expect_status 204 "guest leave"
expect_empty_body "guest leave"
ok

# --- step 16: owner deletes room ------------------------------------------
begin_step 16 DELETE /api/rooms/$ROOM_ID
request DELETE "/api/rooms/$ROOM_ID" '{"requesterId":"smoke-p1"}' "$OWNER_TOKEN"
expect_status 204 "delete room"
expect_empty_body "delete room"
ok

echo "SMOKE OK"
exit 0
