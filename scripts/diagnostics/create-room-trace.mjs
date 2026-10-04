// STEP 6d / D1 - Create Room + Quick Play timing trace. DIAGNOSTIC ONLY.
// No production code is touched. Dev-only helper - NOT part of the e2e suite.
//
// Run from games-frontend/ (the harness resolves paths from process.cwd()):
//   E2E_REBUILD=1 SKIP_BUILD=1 node ../scripts/diagnostics/create-room-trace.mjs
//
// The script forces E2E_REBUILD=1 so scripts/e2e/start_server.sh rebuilds the
// jar before the trace (Step 6d prerequisite). For every game it traces:
//   Path Q: Welcome card -> name prompt -> lobby -> "Quick Play (Solo)" -> game view
//   Path C: Welcome card -> name prompt -> lobby -> "+ Create Room" -> form submit
//           -> "Get Ready" overlay, plus how long a second browser (already
//           polling the list) takes to show the new room row.
// Per scenario it prints: millisecond marks, the /api/ request waterfall
// (method, URL, status, duration), POST headers (Idempotency-Key presence),
// and the React render ticks (MutationObserver coalesced to >=5ms batches)
// inside the measured window.
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const require = createRequire(import.meta.url);
const FRONTEND = process.cwd();
const setupUrl = pathToFileURL(path.join(FRONTEND, 'e2e', 'global-setup.mjs')).href;
const teardownUrl = pathToFileURL(path.join(FRONTEND, 'e2e', 'global-teardown.mjs')).href;

process.env.E2E_REBUILD = '1'; // Step 6d prerequisite: always rebuild the jar
if (!process.env.SKIP_BUILD) process.env.SKIP_BUILD = '1';

const GAMES = [
  {
    key: 'blackjack',
    card: 'Play Blackjack',
    gameSel: '.blackjack-table',
    formHeading: 'Create Blackjack Room',
  },
  {
    key: 'minesweeper',
    card: 'Play Minesweeper',
    gameSel: '.mines-grid',
    formHeading: 'Create Minesweeper Room',
  },
  {
    key: '2048',
    card: 'Play 2048',
    gameSel: '#g2048-up',
    formHeading: 'Create 2048 Room',
  },
];

const now = () => Date.now();

function attachTrace(page, label, events) {
  page.on('dialog', async (d) => {
    events.push({ t: now(), kind: 'dialog', label, type: d.type(), message: d.message() });
    try {
      await d.accept('Tracer');
    } catch {}
    events.push({ t: now(), kind: 'dialog-accepted', label });
  });

  const starts = new Map();
  page.on('request', (req) => starts.set(req, now()));
  page.on('requestfinished', (req) => {
    const t0 = starts.get(req);
    const t1 = now();
    if (!req.url().includes('/api/')) return;
    const entry = {
      t: t1,
      kind: 'net',
      label,
      method: req.method(),
      url: req.url(),
      ms: t0 != null ? t1 - t0 : -1,
      status: null,
    };
    events.push(entry);
    Promise.resolve(req.response())
      .then((res) => {
        entry.status = res ? res.status() : null;
      })
      .catch(() => {});
    if (req.method() !== 'GET') {
      const h = req.headers();
      events.push({
        t: t1,
        kind: 'net-headers',
        label,
        method: req.method(),
        url: req.url(),
        idempotencyKey: h['idempotency-key'] ?? null,
        xPlayerToken: h['x-player-token'] ? 'present' : 'null',
      });
    }
  });
  page.on('requestfailed', (req) => {
    if (!req.url().includes('/api/')) return;
    const t0 = starts.get(req);
    events.push({
      t: now(),
      kind: 'net-failed',
      label,
      method: req.method(),
      url: req.url(),
      ms: t0 != null ? now() - t0 : -1,
      error: req.failure()?.errorText ?? 'unknown',
    });
  });
  page.on('response', (res) => {
    if (res.url().includes('OPTIONS') || res.request().method() === 'OPTIONS') {
      events.push({ t: now(), kind: 'cors-preflight', label, url: res.url(), status: res.status() });
    }
  });
}

async function initTicks(page) {
  await page.addInitScript(() => {
    window.__ticks = [];
    const push = () => {
      const t = Date.now();
      const last = window.__ticks[window.__ticks.length - 1];
      if (last && t - last.t < 5) {
        last.n += 1;
        last.tEnd = t;
        return;
      }
      window.__ticks.push({ t, tEnd: t, n: 1 });
    };
    const mo = new MutationObserver(push);
    const iv = setInterval(() => {
      if (document.documentElement) {
        mo.observe(document.documentElement, {
          subtree: true,
          childList: true,
          attributes: true,
          characterData: true,
        });
        clearInterval(iv);
      }
    }, 1);
  });
}

function printWaterfall(events, from, to, title) {
  console.log(`  -- network waterfall (${title}) --`);
  const net = events
    .filter((e) => (e.kind === 'net' || e.kind === 'net-failed') && e.t >= from && e.t <= to)
    .sort((a, b) => a.t - b.t);
  if (net.length === 0) console.log('    (no /api/ requests in window)');
  for (const e of net) {
    const url = e.url.replace(/^https?:\/\/[^/]+/, '');
    const st = e.kind === 'net-failed' ? `FAILED(${e.error})` : String(e.status);
    console.log(
      `    t+${String(e.t - from).padStart(6)}ms  ${e.method.padEnd(6)} ${url} -> ${st}  [${e.ms}ms]`
    );
  }
  const headers = events.filter(
    (e) => e.kind === 'net-headers' && e.t >= from && e.t <= to && e.method !== 'GET'
  );
  for (const e of headers) {
    const url = e.url.replace(/^https?:\/\/[^/]+/, '');
    console.log(
      `    headers ${e.method ?? ''}${url}  Idempotency-Key=${JSON.stringify(e.idempotencyKey)} X-Player-Token=${e.xPlayerToken}`
    );
  }
}

async function printTicks(page, from, to) {
  const ticks = await page.evaluate(() => window.__ticks ?? []);
  const inWin = ticks.filter((tk) => tk.t >= from && tk.t <= to);
  console.log(`  -- React render ticks in [t+0, t+${to - from}ms]: ${inWin.length} batches --`);
  const shown = inWin.slice(0, 25);
  for (const tk of shown) {
    console.log(`    t+${String(tk.t - from).padStart(6)}ms  mutations=${tk.n}`);
  }
  if (inWin.length > shown.length) console.log(`    ... ${inWin.length - shown.length} more`);
}

async function traceQuickPlay(browser, game, events) {
  console.log(`\n=== D1 PATH Q (Quick Play solo) - ${game.key} ===`);
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  attachTrace(page, `Q:${game.key}`, events);
  await initTicks(page);

  await page.goto('http://127.0.0.1:4179/');
  const tCard = now();
  await page.getByRole('button', { name: game.card }).click();
  await page.getByRole('button', { name: /Quick Play \(Solo\)/ }).waitFor({ timeout: 15000 });
  const tLobby = now();
  console.log(`  card-click -> lobby visible: ${tLobby - tCard} ms (includes name prompt)`);

  const tQp = now();
  await page.getByRole('button', { name: /Quick Play \(Solo\)/ }).click();
  await page.locator(game.gameSel).first().waitFor({ timeout: 15000 });
  const tGame = now();
  console.log(`  quickplay-click -> first game element: ${tGame - tQp} ms`);

  printWaterfall(events, tQp, tGame + 500, `${game.key} path Q`);
  await printTicks(page, tQp, tGame);

  const opts = events.filter(
    (e) => e.kind === 'cors-preflight' && e.label === `Q:${game.key}`
  ).length;
  console.log(`  CORS preflights observed: ${opts}`);
  await ctx.close();
}

async function traceCreateRoom(browser, game, events) {
  console.log(`\n=== D1 PATH C (Create Room form) - ${game.key} ===`);
  const roomName = `Trace ${game.key} ${now()}`;

  const ctxA = await browser.newContext();
  const pageA = await ctxA.newPage();
  attachTrace(pageA, `C:${game.key}:creator`, events);
  await initTicks(pageA);

  const ctxB = await browser.newContext();
  const pageB = await ctxB.newPage();
  attachTrace(pageB, `C:${game.key}:friend`, events);

  await pageA.goto('http://127.0.0.1:4179/');
  await pageA.getByRole('button', { name: game.card }).click();
  await pageA.getByRole('button', { name: '+ Create Room' }).waitFor({ timeout: 15000 });
  const tFormOpen = now();
  await pageA.getByRole('button', { name: '+ Create Room' }).click();
  await pageA.getByRole('heading', { name: game.formHeading }).waitFor({ timeout: 5000 });

  // Friend's lobby is open and polling BEFORE the room exists, so the row can
  // only appear via the 5s list poll - the worst case a real user sees.
  await pageB.goto('http://127.0.0.1:4179/');
  await pageB.getByRole('button', { name: game.card }).click();
  await pageB.getByRole('button', { name: /Quick Play \(Solo\)/ }).waitFor({ timeout: 15000 });
  const tFriendReady = now();
  console.log(`  friend lobby polling since t+${tFriendReady - tFormOpen}ms (before submit)`);

  await pageA.getByLabel('Room Name').fill(roomName);
  const tSubmit = now();
  await pageA.locator('form button[type="submit"]').click();
  await pageA.getByText('Get Ready', { exact: true }).waitFor({ timeout: 15000 });
  const tReady = now();
  console.log(`  form-submit -> "Get Ready" overlay content: ${tReady - tSubmit} ms`);

  const row = pageB.locator(`strong:text-is("${roomName}")`);
  await row.waitFor({ timeout: 15000 });
  const tFriend = now();
  console.log(`  form-submit -> room visible in friend's list: ${tFriend - tSubmit} ms`);

  printWaterfall(events, tSubmit, tReady + 500, `${game.key} creator`);
  await printTicks(pageA, tSubmit, tReady);
  printWaterfall(events, tFormOpen, tFriend + 200, `${game.key} friend`);

  const opts = events.filter(
    (e) => e.kind === 'cors-preflight' && e.label.startsWith(`C:${game.key}`)
  ).length;
  console.log(`  CORS preflights observed: ${opts}`);
  await ctxA.close();
  await ctxB.close();
}

async function main() {
  const { chromium } = require(path.join(FRONTEND, 'node_modules', 'playwright'));
  const setup = (await import(setupUrl)).default;
  const teardown = (await import(teardownUrl)).default;

  await setup();
  const browser = await chromium.launch();
  const events = [];
  try {
    for (const game of GAMES) {
      await traceQuickPlay(browser, game, events);
      await traceCreateRoom(browser, game, events);
    }
  } finally {
    await browser.close();
    await teardown();
  }

  console.log('\n=== D1 GLOBAL CHECKS ===');
  const posts = events.filter((e) => e.kind === 'net-headers' && e.url.includes('/api/rooms'));
  const withIdKey = posts.filter((e) => e.idempotencyKey != null);
  console.log(`  POSTs to /api/rooms(+join/ready/sessions): ${posts.length}`);
  console.log(`  ...carrying an Idempotency-Key header: ${withIdKey.length}`);
  const health = events.filter((e) => e.kind === 'net' && e.url.includes('/actuator'));
  console.log(`  frontend calls to /actuator/*: ${health.length}`);
  const failed = events.filter((e) => e.kind === 'net-failed');
  console.log(`  failed /api/ requests: ${failed.length}`);
  const rate = events.filter((e) => e.kind === 'net' && e.status === 429);
  console.log(`  429 responses: ${rate.length}`);
  const slow = events
    .filter((e) => e.kind === 'net')
    .sort((a, b) => b.ms - a.ms)
    .slice(0, 5);
  console.log('  slowest /api/ requests:');
  for (const e of slow) {
    console.log(
      `    ${e.ms}ms ${e.method} ${e.url.replace(/^https?:\/\/[^/]+/, '')} -> ${e.status}`
    );
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
