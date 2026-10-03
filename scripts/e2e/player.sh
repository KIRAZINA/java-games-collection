#!/usr/bin/env bash
# One simulated player in the multiplayer e2e soak. Drives a full room
# lifecycle over HTTP against the running server: create/join, ready, create
# + register a game session, play the first action, wait for the room's
# expected terminal phase, then leave/delete. Asserts every status code and
# phase transition; prints PLAYER OK / PLAYER FAIL and exits 0/1.
#
# Usage:
#   player.sh owner <pair> <game> <timeLimitSeconds> <workdir>
#   player.sh guest <pair> <game> <workdir>
#     game: MINESWEEPER | TWENTY_FORTY_EIGHT | BLACKJACK
#
# Env:
#   BASE_URL          default http://localhost:8080
#   E2E_CHAOS_FLAG    if set: transport errors / 404s after the flag file
#                     exists mean the server was SIGKILLed by run_soak.sh
#                     --chaos; exit 0 with "interrupted by chaos" instead
#                     of failing (post-restart verification is run_soak's job).
#
# Coordination files written to <workdir>:
#   <pair>.room   "roomId ownerToken"  (owner, after create)
#   <pair>.joined after guest join succeeded
#   <pair>.left    after guest leave succeeded
#   <pair>.done    after owner delete + leak check succeeded

set -u

ROLE="${1:?usage: player.sh owner|guest <pair> <game> [timeLimit] <workdir>}"
PAIR="${2:?pair id}"
GAME="${3:?game type}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

STATUS=""
BODY=""

fail() {
  echo "PLAYER FAIL [$ROLE $PAIR]: $1 (status=$STATUS body=$BODY)"
  exit 1
}

chaos_hit() {
  [ -n "${E2E_CHAOS_FLAG:-}" ] || return 1
  # failure may land microseconds before run_soak touches the flag: recheck
  for _ in 1 2; do
    [ -e "$E2E_CHAOS_FLAG" ] && return 0
    sleep 0.3
  done
  return 1
}

# request <method> <path> [json] [token] - 429 retried, transport errors and
# mid-run 404s routed to chaos handling.
request() {
  local method="$1" path="$2" json="${3-}" token="${4-}"
  local attempt=0
  local -a args=(-sS --max-time 10 -X "$method" -o "$TMP" -w '%{http_code}')
  [ -n "$json" ] && args+=(-H 'Content-Type: application/json' -d "$json")
  [ -n "$token" ] && args+=(-H "X-Player-Token: $token")
  while :; do
    if STATUS="$(curl "${args[@]}" "$BASE_URL$path" 2>/dev/null)"; then
      BODY="$(cat "$TMP")"
      if [ "$STATUS" = 429 ]; then
        attempt=$((attempt + 1))
        if [ "$attempt" -ge 5 ]; then fail "rate limited 429 x$attempt on $method $path"; fi
        sleep 1
        continue
      fi
      return 0
    fi
    BODY=""
    STATUS="curl-error"
    if chaos_hit; then
      echo "PLAYER INTERRUPTED [$ROLE $PAIR]: $method $path during chaos"
      exit 0
    fi
    fail "transport error on $method $path"
  done
}

expect() { # expect <code> <label>
  [ "$STATUS" = "$1" ] || fail "$2: expected HTTP $1"
}

extract() { printf '%s' "$BODY" | sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -n 1; }

# expect_contains <needle> <label>
expect_contains() {
  case "$BODY" in
    *"$1"*) ;;
    *) fail "$2: body missing '$1'" ;;
  esac
}

wait_marker() { # wait_marker <file> <seconds> <label>
  local f="$1" secs="$2" label="$3"
  local i=0
  while [ ! -f "$f" ]; do
    i=$((i + 1))
    [ "$i" -gt "$secs" ] && fail "timeout waiting $label ($f)"
    sleep 1
  done
}

write_marker() { # write_marker <file> <content...>
  local f="$1"; shift
  printf '%s\n' "$*" > "$f.tmp" && mv "$f.tmp" "$f"
}

# wait_phase <state> <max_seconds> <label> - polls GET /state until the room
# reports the expected phase. Also records observed progress so a room that
# never leaves LOBBY (stuck) fails loudly.
wait_phase() {
  local want="$1" max="$2" label="$3"
  local i=0 seen="" seq=""
  while [ "$i" -lt "$max" ]; do
    request GET "/api/rooms/$ROOM/state"
    if [ "$STATUS" = 404 ]; then
      if chaos_hit; then
        echo "PLAYER INTERRUPTED [$ROLE $PAIR]: room vanished during chaos"
        exit 0
      fi
      fail "$label: room 404 while waiting for $want"
    fi
    expect 200 "$label: GET state"
    local st
    st="$(printf '%s' "$BODY" | sed -n 's/.*"state":"\([A-Z_]*\)".*/\1/p' | head -n 1)"
    case "$seq" in *" $st "*) ;; *) seq="$seq $st" ;; esac
    case "$BODY" in *'"playerCount":2'*) saw_two=1 ;; esac
    if [ "$st" = "$want" ]; then
      echo "  [$ROLE $PAIR] reached $want after ${i}s (path:$seq)"
      return 0
    fi
    case "$st" in
      GAME_OVER)
        if [ "$want" != GAME_OVER ]; then
          fail "$label: reached GAME_OVER while waiting for $want (path:$seq)"
        fi
        ;;
      LOBBY)
        if [ "$i" -ge 8 ]; then
          fail "$label: room still LOBBY after ${i}s (stuck room; path:$seq)"
        fi
        ;;
    esac
    i=$((i + 1))
    sleep 1
  done
  fail "$label: did not reach $want within ${max}s (stuck room; path:$seq)"
}

saw_two=0

case "$ROLE" in
owner)
  TIME_LIMIT="${4:?timeLimitSeconds}"
  WORKDIR="${5:?workdir}"
  ROOM=""; TOKEN=""; SESSION=""; PLAYER_ID="soak-$PAIR-own"

  settings_for_game() {
    case "$GAME" in
      MINESWEEPER)          echo "{\"settings\":{\"rows\":9,\"cols\":9,\"mines\":10},\"passwordProtected\":false,\"passwordHash\":null,\"allowBots\":false,\"maxPlayers\":2,\"timeLimitSeconds\":$TIME_LIMIT,\"isSinglePlayer\":false}" ;;
      TWENTY_FORTY_EIGHT)   echo "{\"settings\":{},\"passwordProtected\":false,\"passwordHash\":null,\"allowBots\":false,\"maxPlayers\":2,\"timeLimitSeconds\":$TIME_LIMIT,\"isSinglePlayer\":false}" ;;
      BLACKJACK)            echo "{\"settings\":{\"initialBalance\":100,\"difficulty\":\"BASIC\"},\"passwordProtected\":false,\"passwordHash\":null,\"allowBots\":false,\"maxPlayers\":2,\"timeLimitSeconds\":0,\"isSinglePlayer\":false}" ;;
      *) fail "unknown game $GAME" ;;
    esac
  }

  request POST /api/rooms "{\"roomName\":\"soak-$PAIR\",\"gameType\":\"$GAME\",\"settings\":{\"gameType\":\"$GAME\",$(settings_for_game | sed 's/^{//;s/}$//')},\"ownerId\":\"soak-$PAIR-own\",\"ownerName\":\"Soak Own $PAIR\"}"
  expect 201 "create room"
  ROOM="$(extract roomId)"; TOKEN="$(extract playerToken)"
  [ -n "$ROOM" ] && [ -n "$TOKEN" ] || fail "create room: missing roomId/playerToken"
  expect_contains "\"phase\":\"LOBBY\"" "create room"
  write_marker "$WORKDIR/$PAIR.room" "$ROOM" "$TOKEN"
  echo "  [owner $PAIR] room=$ROOM game=$GAME limit=$TIME_LIMIT"

  wait_marker "$WORKDIR/$PAIR.joined" 15 "guest join"

  request POST "/api/rooms/$ROOM/ready" "{\"playerId\":\"$PLAYER_ID\"}" "$TOKEN"
  expect 200 "owner ready"
  ;;

guest)
  TIME_LIMIT=""
  WORKDIR="${4:?workdir}"
  ROOM=""; TOKEN=""; SESSION=""; PLAYER_ID="soak-$PAIR-gst"
  WAIT_SECS=15

  i=0
  while [ ! -f "$WORKDIR/$PAIR.room" ]; do
    i=$((i + 1))
    [ "$i" -gt "$WAIT_SECS" ] && fail "timeout waiting for room file"
    sleep 1
  done
  ROOM="$(head -n 1 "$WORKDIR/$PAIR.room" | awk '{print $1}')"
  [ -n "$ROOM" ] || fail "room file empty"

  request POST "/api/rooms/$ROOM/join" "{\"playerId\":\"$PLAYER_ID\",\"playerName\":\"Soak Gst $PAIR\"}"
  expect 200 "guest join"
  TOKEN="$(extract playerToken)"
  [ -n "$TOKEN" ] || fail "join: missing playerToken"
  write_marker "$WORKDIR/$PAIR.joined" "joined"

  request POST "/api/rooms/$ROOM/ready" "{\"playerId\":\"$PLAYER_ID\"}" "$TOKEN"
  expect 200 "guest ready"
  ;;

*)
  echo "PLAYER FAIL: unknown role $ROLE" >&2
  exit 1
  ;;
esac

# --- game session: create, register, first action ---------------------------
case "$GAME" in
  MINESWEEPER)
    request POST /api/minesweeper/sessions '{"rows":9,"cols":9,"mines":10}'
    expect 201 "create minesweeper session"
    SESSION="$(extract sessionId)"
    request POST "/api/rooms/$ROOM/sessions" "{\"playerId\":\"$PLAYER_ID\",\"sessionId\":\"$SESSION\"}" "$TOKEN"
    [ "$ROLE" = owner ] && expect 201 "register owner session" || expect 201 "register guest session"
    request POST "/api/minesweeper/sessions/$SESSION/open" '{"row":0,"col":0}'
    expect 200 "first open"
    ;;
  TWENTY_FORTY_EIGHT)
    request POST /api/2048/sessions
    expect 201 "create 2048 session"
    SESSION="$(extract sessionId)"
    request POST "/api/rooms/$ROOM/sessions" "{\"playerId\":\"$PLAYER_ID\",\"sessionId\":\"$SESSION\"}" "$TOKEN"
    expect 201 "register session"
    request POST "/api/2048/sessions/$SESSION/moves" '{"direction":"LEFT"}'
    expect 200 "first move"
    ;;
  BLACKJACK)
    # Blackjack rooms are untimed and lifecycle-only: no rounds are driven
    # here (see README); the room-level assertion is PLAYING after both readys.
    request POST "/api/blackjack/sessions" '{"initialBalance":100,"difficulty":"BASIC"}'
    expect 201 "create blackjack session"
    SESSION="$(extract sessionId)"
    request POST "/api/rooms/$ROOM/sessions" "{\"playerId\":\"$PLAYER_ID\",\"sessionId\":\"$SESSION\"}" "$TOKEN"
    expect 201 "register session"
    ;;
esac

# --- wait for the room's expected terminal phase ----------------------------
if [ "$GAME" = BLACKJACK ]; then
  wait_phase PLAYING 15 "blackjack room start"
else
  wait_phase PLAYING 15 "timed room start"
  wait_phase GAME_OVER $((TIME_LIMIT + 18)) "timed room settle"
fi
[ "$saw_two" = 1 ] || fail "never observed playerCount:2 (registration invisible)"

# --- teardown ----------------------------------------------------------------
if [ "$ROLE" = guest ]; then
  request DELETE "/api/rooms/$ROOM/leave" "{\"playerId\":\"$PLAYER_ID\"}" "$TOKEN"
  expect 204 "guest leave"
  write_marker "$WORKDIR/$PAIR.left" "left"
else
  wait_marker "$WORKDIR/$PAIR.left" 15 "guest leave"
  request DELETE "/api/rooms/$ROOM" '{"requesterId":"soak-'"$PAIR"'-own"}' "$TOKEN"
  expect 204 "owner delete"
  request GET "/api/rooms/$ROOM"
  expect 404 "room gone after delete"
  request GET /api/rooms
  expect 200 "room list"
  case "$BODY" in
    *"soak-$PAIR"*) fail "leaked room soak-$PAIR still listed" ;;
  esac
  write_marker "$WORKDIR/$PAIR.done" "done"
fi

echo "PLAYER OK [$ROLE $PAIR] game=$GAME"
exit 0
