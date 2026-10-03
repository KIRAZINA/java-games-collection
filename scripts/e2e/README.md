# scripts/e2e — multiplayer end-to-end soak harness

Drives the **real running games-backend over HTTP** with concurrent simulated
players — the app as a system under load, not a unit under test. No production
code changes; no new dependencies (bash + curl only).

## Scripts

| script | purpose |
|---|---|
| `start_server.sh` | builds the jar if missing (`mvn -pl games-backend package -DskipTests`), boots `java -jar`, waits for `/actuator/health` (90s), records the **Windows pid** (netstat) in the pid file |
| `stop_server.sh` | `taskkill` (graceful attempt, then `/F`), verifies health is down, removes pid file; idempotent |
| `player.sh` | one simulated player: full two-player room lifecycle with an assertion on every status code and phase transition |
| `run_soak.sh` | orchestrates waves of concurrent `player.sh` pairs, leak check, optional `--chaos` |

## Usage

```bash
# start / stop individually
bash scripts/e2e/start_server.sh          # PORT=8080 default; idempotent
bash scripts/e2e/stop_server.sh

# soak (manages the server lifecycle itself)
bash scripts/e2e/run_soak.sh              # 30s load  -> "SOAK OK"
bash scripts/e2e/run_soak.sh -d 60        # 60s load
bash scripts/e2e/run_soak.sh --chaos      # + SIGKILL/restart + double-ready
                                          #   -> "SOAK OK (chaos)"
PORT=9090 BASE_URL=http://localhost:9090 bash scripts/e2e/run_soak.sh
```

Run from any directory; everything is resolved relative to the repo root.
Requires git-bash (or any bash with curl), `mvn`, `java` on PATH.

## What a player asserts (per pair)

1. `POST /api/rooms` → 201, phase `LOBBY`, roomId + playerToken present.
2. Owner waits for guest join (guest `POST /join` → 200 + token) — join-before-ready
   is required because a 1/1 ready flips phase immediately.
3. Both `POST /ready` with `X-Player-Token` → 200.
4. Game session created (201), registered to the room (`POST /{id}/sessions` → 201),
   first real game action (minesweeper open / 2048 move) → 200.
5. Phase progression polled from `GET /{id}/state`:
   - **MINESWEEPER / TWENTY_FORTY_EIGHT** (timeLimitSeconds=8): must observe
     `READY_CHECK` → `PLAYING` → `GAME_OVER` (scheduler-driven; 3s ready-check,
     then time limit). A room stuck in `LOBBY` >8s fails as **stuck room**;
     reaching `GAME_OVER` while waiting for `PLAYING` fails too.
   - **BLACKJACK** (untimed): must reach `PLAYING` (starts on both ready; no
     GAME_OVER path — blackjack rooms are lifecycle-only here, no rounds driven).
6. `playerCount:2` must have been observed (registration visible).
7. Teardown: guest `DELETE /leave` → 204, owner `DELETE` room → 204 →
   `GET` room → 404, `GET /api/rooms` must not list `soak-*` (leak check).

Waves rotate the three game types; every request retries 429 (rate limit =
30 non-GET req/10s per IP; GETs are exempt) up to 5 times.

## Chaos mode (`--chaos`)

- Mid-load: `kill -9` + `taskkill //F` the server (**Windows SIGKILL
  equivalent**, `//` → `/` after msys conversion), verifies health is
  unreachable, touches a flag file, restarts via `start_server.sh`.
- In-flight players that fail after the flag report `PLAYER INTERRUPTED`
  (exit 0) instead of failing — restart correctness is asserted afterwards.
- **Double-ready scenario** on the fresh server: concurrent ready ×2 → both
  must be 200 (server serializes them); once the phase leaves `LOBBY`, a late
  duplicate ready must be **409 CONFLICT** (`IllegalStateException` handler —
  never a 5xx); the room must still settle to `GAME_OVER` (no stuck room);
  teardown leaves no leak.

## TTL notes (why TTLs are not asserted)

Soak windows are seconds; the TTL constants are much longer and are not
configurable properties: game sessions `SESSION_TTL = 30 min`
(`games.*.cleanup-delay-ms` is the purge *schedule*, rolling `lastTouched`),
rooms `ROOM_TTL = 2 h` / `EMPTY_ROOM_TTL = 5 min` (and a room is removed
immediately when its last player leaves). Leak-freedom is therefore asserted
at room level after every run instead.

## Exit contract

- all players OK, leak check clean → prints `SOAK OK`, exit 0
- chaos mode additionally → `SOAK OK (chaos)`, exit 0
- any `PLAYER FAIL`, stuck room, leaked room, failed scenario → diagnostics on
  stderr, exit 1

## Known findings

- none — the former finding (`POST /api/rooms` with a missing top-level
  `ownerId` returned **500** instead of 400) was fixed in Step 6b Part A:
  `CreateRoomRequest` now carries bean validation (`@NotBlank ownerId`,
  `@NotBlank roomName`, `@NotBlank ownerName`, `@NotNull gameType`) and
  `GlobalExceptionHandler` maps `MethodArgumentNotValidException` to
  **400** `{"error":"ownerId: must not be blank","status":400}`. Verified by
  `DtoValidationTest` and a 21-case before/after probe matrix (raw evidence:
  `%TEMP%\step6b\pre-fix.txt`, `%TEMP%\step6b\post-fix.txt`).
