# STEP 6b - canonical step spec

> Retroactive capture (curator process change, issued with the Part B re-paste):
> from this step onward a step prompt longer than ~500 words is written to
> `docs/step-specs/STEP_<id>.md` on the first action of the step and referenced
> from the CHANGELOG. This file was reconstructed for 6b after the agent lost
> the original prompt: **Part A below is the executed scope** (what the agent
> implemented and the curator's recorded decisions), **Part B below is verbatim
> curator text** re-pasted after the loss. Mid-step revisions are made by
> dictated replacement blocks; the diff of this file is part of acceptance.

## Part A - DTO null-safety / queue fix (executed scope)

Finding being closed (reported by Step 6a Part B, documented in
`scripts/e2e/README.md`): `POST /api/rooms` with a missing top-level `ownerId`
returned **500** (`ConcurrentHashMap` null-key NPE via
`GameRoomService.createRoom` / `issuePlayerToken`) instead of 400.

Executed scope:

1. Bean validation (`jakarta.validation`) on request DTOs across
   `blackjack/`, `minesweeper/`, `twentyfortyeight/`, `common/` controllers -
   sweep every request DTO, document each field's pre-fix behavior and action
   (or why not).
2. Add the validation implementation dependency to the backend pom.
3. `@Valid` on controller `@RequestBody` parameters.
4. `MethodArgumentNotValidException` handler returning
   `{"error": "<field>: <message>", "status": 400}` (same map shape as the
   existing 404/405/403 handlers); the error must name the offending field
   (e.g. `ownerId: must not be blank`).
5. New `DtoValidationTest` with **exactly 6 tests** (test-count arithmetic:
   249 baseline + 6 = 255).
6. `scripts/e2e/README.md` known-finding removal + CHANGELOG STEP 6b section.
7. No successful-response JSON shape changes; no existing validation changed;
   no test removed or weakened; no gameplay changes.

Curator decisions recorded mid-step:

- **`settings` stays optional**: spec's `@NotNull` on nested `settings`
  conflicts with ~10 existing green tests and the controller's deliberate
  `defaultFor` fallback. Curator chose "Keep defaulting": `@Valid` cascade on
  `settings` only, no `@NotNull`.
- New production dependency `spring-boot-starter-validation` accepted (bean
  validation requires an implementation; the "no production dependencies"
  constraint scopes Part B tooling).

### Part A acceptance - curator's required artifacts (bundle with Part B report)

1. The three raw `mvn test -pl games-backend` outputs (normal priority) that
   produce 255/0/0/0 - the raw `Results:` / `BUILD SUCCESS` block from each run.
2. The DTO validation sweep table: every request DTO across `blackjack/`,
   `minesweeper/`, `twentyfortyeight/`, `common/`, with columns: DTO name -
   has-validation? - what was added - why, if not.
3. The diffs for: `CreateRoomRequest.java`, `GlobalExceptionHandler.java`,
   whichever controller(s) got `@Valid`, and the new `DtoValidationTest.java`.
4. The CHANGELOG STEP 6b section.
5. Two explanations: (a) the locale finding in `374eaa8` - what it is, what
   the test asserts, real behavior change vs cosmetic, explicit pin; (b) the
   21-case probe vs the spec's 6 `DtoValidationTest` cases - what the extra 15
   assert and why.
6. Confirmation that `POST /api/rooms` with `ownerId` omitted now returns 400
   with `error` mentioning `ownerId` - the actual 6a finding closed.

## Part B - real-browser Playwright e2e (verbatim curator text)

```
  B1. Quick-play Blackjack, single player, full round
      - Load the app, click "Quick Play (Solo)" for Blackjack
      - Assert: BETTING phase, bet input accepts 10, Place Bet -> PLAYER_TURN,
        click Stand -> ROUND_OVER, balance updated, notifications panel
        renders (may be empty), New Round -> BETTING again
      - Assert: no console.error during the round

  B2. Two-player Minesweeper room, real browsers (two pages)
      - Page A: create a Minesweeper room (public, 2 players)
      - Page B: list rooms, join the room by name
      - Both: click "I'm Ready!"
      - Wait for READY_CHECK -> PLAYING
      - Page A: click an unsafe-looking cell; Page B: flag a different cell
      - Assert: both pages see the same score / phase in the room progress
        panel within 5s (polling works in real browsers)

  B3. Two-player 2048 room, real browsers
      - Same as B2 but 2048; both players make one move; the room settles
        to GAME_OVER after the 8s time limit (verify both pages see GAME_OVER
        and a winner)

  B4. Backend restart mid-game
      - Page A joins a 2048 room and reaches PLAYING
      - Kill the backend (taskkill //F), start it again on the SAME port with
        a fresh game state
      - Page A must observe within 10s: the poll error UI
        ("Lost connection to the room. Please exit and try again.") and the
        Exit Room button

  B5. Token loss (sessionStorage cleared)
      - Page A joins a room, reaches LOBBY
      - In Page A, evaluate sessionStorage.clear()
      - Click "I'm Ready!" -> server 403
      - Assert: notice "Please rejoin the room." appears, the page returns to
        the room list, and the room list loads normally

  B6. Two-tab isolation sanity check
      - Tab A: create a room as player-a
      - Tab B: create a room as player-b
      - Assert: neither tab's action mutates the other's room state; both
        rooms remain separate in the list
```

### TEST INFRASTRUCTURE REQUIREMENTS (verbatim)

```
TEST INFRASTRUCTURE REQUIREMENTS
  - A Playwright globalSetup that:
      * starts the backend (bash scripts/e2e/start_server.sh with PORT=reuse
        of the existing server if one is already up, otherwise random port)
      * builds the frontend (npm run build) unless SKIP_BUILD=1
      * serves the dist folder on a fixed test port (use `npx serve dist -l
        <port>` or a small inline Node http-server; do not add a heavyweight
        dependency)
      * tears everything down in globalTeardown
  - Fail-fast assertions; each test has a clear timeout (30s default,
    60s for B4)
  - Tests must not be flaky under CPU load: no fixed sleeps; use
    page.waitForSelector / expect.poll / waitForFunction throughout
```

### Part B acceptance (verbatim)

```
- Raw `npm run e2e` output showing B1–B6 green.
- Full source for each of B1–B6 — I want to read the assertions, not a summary.
- One specific answer: **was there anything jsdom-passed-but-browser-failed?**
  That is the entire reason B exists. If nothing differs, say so plainly; if
  something differs, that difference is the finding.
- Playwright report description.
- CHANGELOG STEP 6b section covering both Part A and Part B.
- New frontend e2e test count with arithmetic.
```

Constraints (from the step prompt): no production dependency additions beyond
the Part A validation starter (Playwright/serve dev-only); no gameplay rule
changes; commit-before-modify for every file; if Playwright is not installable
STOP (no jsdom/fetch-mock substitute); if any B scenario fails, report which
and why BEFORE any fix; B-series is zero production code fixes - frontend bugs
surfaced by B4/B5/B6 are reported, not fixed.
