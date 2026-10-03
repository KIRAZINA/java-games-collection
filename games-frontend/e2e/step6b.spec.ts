// STEP 6b Part B - real-browser Playwright scenarios B1-B6 (docs/step-specs/STEP_6b.md).
//
// Infrastructure: globalSetup starts/reuses the backend, builds the frontend
// and serves dist on the fixed test port 4179 with an /api proxy; one worker,
// no parallelism (shared backend, B4 restarts it). No fixed sleeps anywhere:
// waitForSelector/expect polling only. No production code is touched.
import { test, expect, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import path from 'node:path';

const ROOT = path.resolve(process.cwd(), '..');
const RUN = Date.now();

function sh(script: string): string {
  return execFileSync(process.env.E2E_BASH ?? 'bash', ['-c', script], {
    cwd: ROOT,
    env: process.env,
    encoding: 'utf8',
  });
}

function acceptPrompt(page: Page, name: string) {
  page.on('dialog', (d) => {
    d.accept(name).catch(() => {});
  });
}

function captureConsole(page: Page) {
  const consoleErrors: string[] = [];
  const pageErrors: string[] = [];
  page.on('console', (m) => {
    if (m.type() === 'error') {
      consoleErrors.push(`${m.text()} @ ${m.location()?.url ?? '?'}`);
    }
  });
  page.on('pageerror', (e) => pageErrors.push(String(e)));
  return { consoleErrors, pageErrors };
}

const headerMeta = (page: Page) => page.locator('header.game-header span');

test('B1 - quick-play Blackjack, single player, full round', async ({ page }) => {
  const { consoleErrors, pageErrors } = captureConsole(page);
  acceptPrompt(page, `Alice-${RUN}`);
  await page.goto('/');

  // Load the app, click "Quick Play (Solo)" for Blackjack
  await page.getByRole('button', { name: 'Play Blackjack' }).click();
  await page.getByRole('button', { name: /Quick Play \(Solo\)/ }).click();

  // Assert: BETTING phase
  await expect(headerMeta(page)).toHaveText('BETTING', { timeout: 15000 });

  // Assert: bet input accepts 10
  const betInput = page.locator('#bj-bet-amount');
  await betInput.fill('10');
  await expect(betInput).toHaveValue('10');
  const balance = page.locator('.balance-amount');
  await expect(balance).toHaveText('$100.00');

  // Assert: Place Bet -> PLAYER_TURN.
  // placeBet() auto-settles on naturals (Dealer Peek, BlackjackSession.java:74-83),
  // so a natural lands on ROUND_OVER instead - retry with New Round until a
  // normal hand reaches PLAYER_TURN (bounded, deterministic in practice).
  let reachedPlayerTurn = false;
  for (let attempt = 0; attempt < 8 && !reachedPlayerTurn; attempt++) {
    await expect(headerMeta(page)).toHaveText('BETTING');
    await page.locator('#bj-place-bet').click();
    await expect(headerMeta(page)).toHaveText(/PLAYER_TURN|ROUND_OVER/, { timeout: 10000 });
    reachedPlayerTurn = (await headerMeta(page).textContent()) === 'PLAYER_TURN';
    if (!reachedPlayerTurn) await page.locator('#bj-new-round').click();
  }
  expect(reachedPlayerTurn, 'Place Bet must reach PLAYER_TURN (retried naturals)').toBe(true);

  // Assert: balance updated by the bet (placeBet always deducts, BlackjackSession.java:67)
  await expect(balance).not.toHaveText('$100.00');

  // Assert: click Stand -> ROUND_OVER
  await page.locator('#bj-stand').click();
  await expect(headerMeta(page)).toHaveText('ROUND_OVER');

  // Assert: notifications panel renders (may be empty).
  // Product behavior: notifications exist only for naturals / 5-card charlie /
  // balance refill (BlackjackSession addNotification call sites) - a normal
  // settle renders no panel at all, which the spec's "(may be empty)" allows.
  // What must never render is an error line.
  const notificationPanels = await page.locator('div[role="alert"]').count();
  test.info().annotations.push({
    type: 'notifications-panel',
    description:
      notificationPanels > 0
        ? 'rendered with content'
        : 'absent - normal settle emits no notifications (product behavior)',
  });
  expect(notificationPanels).toBeLessThanOrEqual(1);
  await expect(page.locator('p.error-line[role="alert"]')).toHaveCount(0);

  // Assert: New Round -> BETTING again
  await page.locator('#bj-new-round').click();
  await expect(headerMeta(page)).toHaveText('BETTING');

  // Assert: no console.error during the round (favicon noise excluded)
  const errors = consoleErrors.filter((t) => !/favicon/i.test(t));
  expect(errors, `console.error during the round: ${errors.join(' | ')}`).toEqual([]);
  expect(pageErrors, `uncaught page errors: ${pageErrors.join(' | ')}`).toEqual([]);
});

test('B2 - two-player Minesweeper room, real browsers (two pages)', async ({ browser }) => {
  const roomName = `B2 Room ${RUN}`;
  const nameA = `player-a-${RUN}`;
  const nameB = `player-b-${RUN}`;

  // Page A: create a Minesweeper room (public, 2 players)
  const ctxA = await browser.newContext();
  const pageA = await ctxA.newPage();
  acceptPrompt(pageA, nameA);
  await pageA.goto('/');
  await pageA.getByRole('button', { name: 'Play Minesweeper' }).click();
  await pageA.getByRole('button', { name: '+ Create Room' }).click();
  await pageA.locator('form').getByLabel('Room Name').fill(roomName);
  await pageA.locator('form button[type="submit"]').click();
  await expect(pageA.getByText('Waiting for players')).toBeVisible({ timeout: 15000 });

  // Page B: list rooms, join the room by name
  const ctxB = await browser.newContext();
  const pageB = await ctxB.newPage();
  acceptPrompt(pageB, nameB);
  await pageB.goto('/');
  await pageB.getByRole('button', { name: 'Play Minesweeper' }).click();
  const nameCol = pageB.locator(`div:has(> strong:text-is("${roomName}"))`);
  await expect(nameCol).toContainText('1/2 players', { timeout: 15000 });
  await nameCol.locator('..').getByRole('button', { name: 'Join' }).click();
  await expect(pageB.getByText('Waiting for players')).toBeVisible({ timeout: 15000 });

  // Both: click "I'm Ready!"
  const readyA = pageA.getByRole('button', { name: "I'm Ready!" });
  const readyB = pageB.getByRole('button', { name: "I'm Ready!" });
  await expect(readyA).toBeVisible({ timeout: 15000 });
  await readyA.click();
  await expect(readyB).toBeVisible({ timeout: 15000 });
  await readyB.click();

  // Wait for READY_CHECK -> PLAYING (both pages leave the waiting overlay)
  await expect(pageA.locator('.mines-grid')).toBeVisible({ timeout: 15000 });
  await expect(pageB.locator('.mines-grid')).toBeVisible({ timeout: 15000 });
  await expect(pageA.locator('.timer-display')).toBeVisible({ timeout: 5000 });
  await expect(pageB.locator('.timer-display')).toBeVisible({ timeout: 5000 });

  // Page A: click an unsafe-looking cell; Page B: flag a different cell
  await pageA.locator('#ms-cell-4-4').click();
  await pageB.locator('#ms-cell-0-0').click({ button: 'right' });

  // Assert: both pages see the same score / phase in the room progress panel
  // within 5s (polling works in real browsers)
  const ownStrip = (p: Page) => p.locator('div.status-strip:not(.opponent-strip)');
  const scoreOf = (txt: string | null) => Number(/Score:\s*(\d+)/.exec(txt ?? '')?.[1] ?? NaN);
  await expect
    .poll(
      async () => {
        const aViewB = scoreOf(await pageA.locator('.opponent-strip').filter({ hasText: nameB }).textContent());
        const bOwn = scoreOf(await ownStrip(pageB).textContent());
        const bViewA = scoreOf(await pageB.locator('.opponent-strip').filter({ hasText: nameA }).textContent());
        const aOwn = scoreOf(await ownStrip(pageA).textContent());
        return (
          !Number.isNaN(aViewB) && !Number.isNaN(bViewA) && aViewB === bOwn && bViewA === aOwn
        );
      },
      { timeout: 5000 }
    )
    .toBe(true);

  await ctxA.close();
  await ctxB.close();
});

test('B3 - two-player 2048 room settles to GAME_OVER after the 8s time limit', async ({
  browser,
  request,
}) => {
  const roomName = `B3 Room ${RUN}`;

  // The Time Limit select only offers 30/60/180 seconds (RoomLobby CreateRoomForm),
  // so an 8s room cannot be created through the UI - it is created through the
  // API with timeLimitSeconds: 8. maxPlayers: 3 because the API-created owner
  // occupies a slot and both UI players must still be able to join.
  const created = await request.post('/api/rooms', {
    data: {
      roomName,
      gameType: 'TWENTY_FORTY_EIGHT',
      settings: {},
      passwordProtected: false,
      passwordHash: '',
      allowBots: false,
      maxPlayers: 3,
      timeLimitSeconds: 8,
      isSinglePlayer: false,
      ownerId: `api-owner-${RUN}`,
      ownerName: 'ApiOwner',
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  const roomId = (await created.json()).roomId as string;

  // Both players join through the UI (same as B2)
  const ctxA = await browser.newContext();
  const pageA = await ctxA.newPage();
  acceptPrompt(pageA, `player-a-${RUN}`);
  await pageA.goto('/');
  await pageA.getByRole('button', { name: 'Play 2048' }).click();
  const colA = pageA.locator(`div:has(> strong:text-is("${roomName}"))`);
  await expect(colA).toContainText('1/3 players', { timeout: 15000 });
  await colA.locator('..').getByRole('button', { name: 'Join' }).click();
  await expect(pageA.getByText('Waiting for players')).toBeVisible({ timeout: 15000 });

  const ctxB = await browser.newContext();
  const pageB = await ctxB.newPage();
  acceptPrompt(pageB, `player-b-${RUN}`);
  await pageB.goto('/');
  await pageB.getByRole('button', { name: 'Play 2048' }).click();
  const colB = pageB.locator(`div:has(> strong:text-is("${roomName}"))`);
  await expect(colB).toContainText('2/3 players', { timeout: 15000 });
  await colB.locator('..').getByRole('button', { name: 'Join' }).click();
  await expect(pageB.getByText('Waiting for players')).toBeVisible({ timeout: 15000 });

  // Both: click "I'm Ready!"
  const readyA = pageA.getByRole('button', { name: "I'm Ready!" });
  const readyB = pageB.getByRole('button', { name: "I'm Ready!" });
  await expect(readyA).toBeVisible({ timeout: 15000 });
  await readyA.click();
  await expect(readyB).toBeVisible({ timeout: 15000 });
  await readyB.click();

  // Wait for READY_CHECK -> PLAYING
  await expect(pageA.locator('#g2048-up')).toBeVisible({ timeout: 15000 });
  await expect(pageB.locator('#g2048-up')).toBeVisible({ timeout: 15000 });

  // Both players make one move (a fresh board always accepts at least one
  // of the four directions; bounded retries, no sleeps)
  const makeOneMove = async (page: Page) => {
    const ownMoves = page.locator('div.status-strip:not(.opponent-strip)');
    for (const id of ['#g2048-up', '#g2048-left', '#g2048-right', '#g2048-down']) {
      await page.locator(id).click();
      try {
        await expect(ownMoves).toContainText(/Moves: [1-9]/, { timeout: 2500 });
        return;
      } catch {
        /* try next direction */
      }
    }
    throw new Error('no 2048 move was accepted on a fresh board');
  };
  await makeOneMove(pageA);
  await makeOneMove(pageB);

  // The room settles to GAME_OVER after the 8s time limit - both pages see it
  await expect(pageA.getByText("Time's Up!")).toBeVisible({ timeout: 20000 });
  await expect(pageB.getByText("Time's Up!")).toBeVisible({ timeout: 20000 });
  const state = await request.get(`/api/rooms/${roomId}/state`);
  expect((await state.json()).roomPhase).toBe('GAME_OVER');

  // "and a winner": settleGame() computes winnerId/winnerScore (GameRoomService:370-401),
  // but neither RoomStateResponse, RoomProgressResponse, RoomSummary nor any UI
  // element exposes it - nothing observable to assert beyond GAME_OVER above
  // (reported as a finding, not fixed: B-series is zero production fixes).

  await ctxA.close();
  await ctxB.close();
});

test('B4 - backend restart mid-game surfaces the poll error UI', async ({ browser }) => {
  test.setTimeout(60_000);
  const page = await (await browser.newContext()).newPage();
  acceptPrompt(page, `player-a-${RUN}`);
  await page.goto('/');

  // Page A reaches a 2048 room in PLAYING (single-player rooms start PLAYING
  // immediately: GameRoomService.createRoom marks the owner ready + startGame)
  await page.getByRole('button', { name: '2048 (Solo)' }).click();
  await expect(page.locator('#g2048-up')).toBeVisible({ timeout: 15000 });

  // Kill the backend (taskkill //F) - Windows pid from the PID file, netstat fallback
  const port = process.env.E2E_BACKEND_PORT!;
  const pid = sh(
    `PID=$(cat "\${TMPDIR:-/tmp}/e2e-games-server.pid" 2>/dev/null); ` +
      `if [ -z "$PID" ]; then PID=$(netstat -ano | grep LISTENING | grep ":${port} " | head -n 1 | awk '{print $NF}'); fi; ` +
      `echo "$PID"`
  ).trim();
  expect(pid, 'backend pid for taskkill //F').toMatch(/^\d+$/);
  sh(`taskkill //F //PID ${pid}`);

  // Assert within 10s of the kill: the poll error UI (3 failed polls latch it -
  // waiting for it BEFORE restarting guarantees the 404 "room no longer exists"
  // path cannot race the connection-refused path)
  const pollError = page
    .getByRole('alert')
    .filter({ hasText: 'Lost connection to the room. Please exit and try again.' });
  await expect(pollError).toBeVisible({ timeout: 10000 });
  const exitRoom = page.getByRole('button', { name: 'Exit Room' });
  await expect(exitRoom).toBeVisible({ timeout: 10000 });

  // Start it again on the SAME port with a fresh game state
  const restarted = sh(`PORT=${port} bash scripts/e2e/start_server.sh`);
  process.stdout.write(restarted);
  const health = await page.request.get(`http://127.0.0.1:${port}/actuator/health`);
  expect(health.status(), 'backend health after restart').toBe(200);

  // The poll error UI and Exit Room button remain observable after the restart
  await expect(pollError).toBeVisible({ timeout: 5000 });
  await expect(exitRoom).toBeVisible({ timeout: 5000 });
});

test('B5 - token loss (sessionStorage cleared) returns the player to the room list', async ({
  browser,
}) => {
  const page = await (await browser.newContext()).newPage();
  acceptPrompt(page, `player-a-${RUN}`);
  await page.goto('/');

  // Page A enters a room whose lobby hosts the "I'm Ready!" button - a
  // single-player practice room is the only room type that renders the
  // ReadyCheckOverlay (RoomLobby.jsx:282/306).
  await page.getByRole('button', { name: 'Play Minesweeper' }).click();
  await page.getByRole('button', { name: '+ Create Room' }).click();
  const form = page.locator('form');
  await form.getByLabel('Room Name').fill(`B5 Room ${RUN}`);
  await form.locator('input[type="checkbox"]').check();
  await form.locator('button[type="submit"]').click();

  // Diagnostic: in a real browser the solo room is created already in PLAYING
  // phase (GameRoomService.createRoom auto-ready + startGame), so the lobby
  // overlay is skipped and the board appears instead. The jsdom counterpart
  // (TokenLoss.test.jsx) stubs GET /state as LOBBY, which is why it passes.
  await expect(page.locator('.mines-grid')).toBeVisible({ timeout: 15000 });
  test.info().annotations.push({
    type: 'finding-1',
    description:
      'solo room auto-starts PLAYING - ReadyCheckOverlay (the only home of "I\'m Ready!") never renders',
  });

  // In Page A, evaluate sessionStorage.clear()
  await page.evaluate(() => sessionStorage.clear());
  expect(await page.evaluate(() => sessionStorage.getItem('roomTokens'))).toBeNull();

  // Diagnostic: the next authenticated poll still carries X-Player-Token and
  // succeeds - api.js caches roomTokens in module scope (api.js:4-28), so a
  // storage-only clear cannot produce the 403 the spec expects. The jsdom test
  // passes because it explicitly calls rehydrateRoomTokens() after clear()
  // (TokenLoss.test.jsx:61-62) and stubs /ready as 403.
  const nextPoll = page.waitForResponse((r) => r.url().includes('/state'), { timeout: 5000 });
  const resp = await nextPoll;
  expect(resp.status()).toBe(200);
  expect(resp.request().headers()['x-player-token']).toBeTruthy();
  test.info().annotations.push({
    type: 'finding-2',
    description: 'sessionStorage cleared but app still authenticates (in-memory token cache)',
  });

  // Click "I'm Ready!" -> server 403 -> notice "Please rejoin the room." ->
  // back to the room list (spec steps; unreachable in the real browser)
  const ready = page.getByRole('button', { name: "I'm Ready!" });
  await expect(ready).toBeVisible({ timeout: 15000 });
  await ready.click();
  await expect(page.getByText('Please rejoin the room.')).toBeVisible({ timeout: 5000 });
  await expect(page.getByRole('button', { name: 'Join' })).toBeVisible({ timeout: 5000 });
});

test('B6 - two-tab isolation sanity check', async ({ browser }) => {
  const alpha = `Room-Alpha-${RUN}`;
  const bravo = `Room-Bravo-${RUN}`;

  // Tab A: create a room as player-a
  const ctxA = await browser.newContext();
  const pageA = await ctxA.newPage();
  acceptPrompt(pageA, `player-a-${RUN}`);
  await pageA.goto('/');
  await pageA.getByRole('button', { name: 'Play Minesweeper' }).click();
  await pageA.getByRole('button', { name: '+ Create Room' }).click();
  await pageA.locator('form').getByLabel('Room Name').fill(alpha);
  await pageA.locator('form button[type="submit"]').click();
  await expect(pageA.getByText('Waiting for players')).toBeVisible({ timeout: 15000 });

  // Tab B: sees A's room in the list, then creates its own room as player-b
  const ctxB = await browser.newContext();
  const pageB = await ctxB.newPage();
  acceptPrompt(pageB, `player-b-${RUN}`);
  await pageB.goto('/');
  await pageB.getByRole('button', { name: 'Play Minesweeper' }).click();
  const alphaColB = pageB.locator(`div:has(> strong:text-is("${alpha}"))`);
  await expect(alphaColB).toContainText('1/2 players', { timeout: 15000 });
  await expect(alphaColB.locator('..').getByRole('button', { name: 'Join' })).toBeEnabled();

  await pageB.getByRole('button', { name: '+ Create Room' }).click();
  await pageB.locator('form').getByLabel('Room Name').fill(bravo);
  await pageB.locator('form button[type="submit"]').click();
  await expect(pageB.getByText('Waiting for players')).toBeVisible({ timeout: 15000 });

  // Assert: neither tab's action mutated the other's room state -
  // A is untouched, and both rooms exist separately in the list
  await expect(pageA.getByText('Waiting for players')).toBeVisible({ timeout: 5000 });
  const list = await (await pageB.request.get('/api/rooms?type=MINESWEEPER')).json();
  const roomAlpha = list.find((r: { roomName: string }) => r.roomName === alpha);
  const roomBravo = list.find((r: { roomName: string }) => r.roomName === bravo);
  expect(roomAlpha, `room ${alpha} in list`).toBeTruthy();
  expect(roomBravo, `room ${bravo} in list`).toBeTruthy();
  expect(roomAlpha.roomId).not.toBe(roomBravo.roomId);
  expect(roomAlpha.playerCount).toBe(1);
  expect(roomBravo.playerCount).toBe(1);
  expect(roomAlpha.phase).toBe('LOBBY');
  expect(roomBravo.phase).toBe('LOBBY');

  // B leaves its own room (Home) - only B's room may disappear; A's room must
  // still be listed untouched in B's real UI room list afterwards
  await pageB.getByRole('button', { name: 'Home' }).click();
  await pageB.getByRole('button', { name: 'Play Minesweeper' }).click();
  const alphaAfter = pageB.locator(`div:has(> strong:text-is("${alpha}"))`);
  await expect(alphaAfter).toContainText('1/2 players', { timeout: 15000 });
  await expect(pageB.locator(`strong:text-is("${bravo}")`)).toHaveCount(0);

  // A's room state is still untouched server-side
  const alphaState = await (await pageA.request.get(`/api/rooms/${roomAlpha.roomId}`)).json();
  expect(alphaState.playerCount).toBe(1);
  expect(alphaState.phase).toBe('LOBBY');
  await expect(pageA.getByText('Waiting for players')).toBeVisible({ timeout: 5000 });

  await ctxA.close();
  await ctxB.close();
});
