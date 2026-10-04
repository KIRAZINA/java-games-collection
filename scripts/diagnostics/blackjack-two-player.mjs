// STEP 6d / D2 + D3 + D4 - multiplayer blackjack room diagnostic. DIAGNOSTIC ONLY.
// No production code is touched. Dev-only helper - NOT part of the e2e suite.
//
// Run from games-frontend/ (the harness resolves paths from process.cwd()):
//   E2E_REBUILD=1 SKIP_BUILD=1 node ../scripts/diagnostics/blackjack-two-player.mjs
//
// Two real browser contexts play a full two-player flow:
//   D3: room phase snapshots (GET /state, GET /progress) at every ready
//       transition: before any ready, +6.5s (no-auto-advance proof), after
//       A readies, right after B readies, and after sessions register.
//       Includes a field-level diff per transition.
//   D2: with both players mid-round (2+ cards each), dumps the raw JSON
//       bodies of GET /state and GET /progress (verbatim + %TEMP%\step6d\).
//   D4: per-player screen facts (phase pill, button disabled states,
//       opponent strips) and the full /api/ network trace for player B,
//       including when B's BlackjackSession is created and registered.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const require = createRequire(import.meta.url);
const FRONTEND = process.cwd();
const setupUrl = pathToFileURL(path.join(FRONTEND, 'e2e', 'global-setup.mjs')).href;
const teardownUrl = pathToFileURL(path.join(FRONTEND, 'e2e', 'global-teardown.mjs')).href;

process.env.E2E_REBUILD = '1'; // Step 6d prerequisite: always rebuild the jar
if (!process.env.SKIP_BUILD) process.env.SKIP_BUILD = '1';

const OUT_DIR = path.join(os.tmpdir(), 'step6d');
const now = () => Date.now();

function attachNetLog(page, label, events) {
  page.on('dialog', async (d) => {
    try {
      await d.accept(label === 'A' ? 'Alice' : 'Bob');
    } catch {}
  });
  const starts = new Map();
  page.on('request', (req) => starts.set(req, now()));
  page.on('requestfinished', (req) => {
    if (!req.url().includes('/api/')) return;
    const t0 = starts.get(req);
    const t1 = now();
    const e = {
      t: t1,
      label,
      method: req.method(),
      url: req.url().replace(/^https?:\/\/[^/]+/, ''),
      ms: t0 != null ? t1 - t0 : -1,
      status: null,
    };
    events.push(e);
    Promise.resolve(req.response())
      .then((res) => {
        e.status = res ? res.status() : null;
      })
      .catch(() => {});
  });
  page.on('requestfailed', (req) => {
    if (!req.url().includes('/api/')) return;
    events.push({
      t: now(),
      label,
      method: req.method(),
      url: req.url().replace(/^https?:\/\/[^/]+/, ''),
      ms: -1,
      status: 'FAILED ' + (req.failure()?.errorText ?? ''),
    });
  });
}

async function api(pathname) {
  const base = process.env.E2E_BACKEND_URL;
  const res = await fetch(`${base}${pathname}`);
  const text = await res.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = text;
  }
  return { status: res.status, body };
}

function flatten(o, prefix = '', out = {}) {
  if (o === null || typeof o !== 'object') {
    out[prefix] = o;
    return out;
  }
  if (Array.isArray(o)) {
    if (o.length === 0) out[prefix] = '(empty array)';
    o.forEach((v, i) => flatten(v, `${prefix}[${i}]`, out));
    return out;
  }
  for (const [k, v] of Object.entries(o)) flatten(v, prefix ? `${prefix}.${k}` : k, out);
  return out;
}

function diffFlat(a, b) {
  const fa = flatten(a);
  const fb = flatten(b);
  const keys = [...new Set([...Object.keys(fa), ...Object.keys(fb)])].sort();
  const rows = [];
  for (const k of keys) {
    const va = k in fa ? JSON.stringify(fa[k]) : '(absent)';
    const vb = k in fb ? JSON.stringify(fb[k]) : '(absent)';
    if (va !== vb) rows.push(`      ${k}: ${va} -> ${vb}`);
  }
  return rows;
}

async function snapshot(roomId, name, prev) {
  const st = (await api(`/api/rooms/${roomId}/state`)).body;
  const pr = (await api(`/api/rooms/${roomId}/progress`)).body;
  console.log(`\n--- SNAPSHOT ${name} : GET /state (verbatim) ---`);
  console.log(JSON.stringify(st, null, 2));
  console.log(`--- SNAPSHOT ${name} : GET /progress (verbatim) ---`);
  console.log(JSON.stringify(pr, null, 2));
  if (prev) {
    const d = diffFlat(prev.st, st);
    console.log(`  /state field changes vs previous snapshot:`);
    console.log(d.length ? d.join('\n') : '      (none)');
  }
  return { st, pr };
}

async function screenFacts(page) {
  return page.evaluate(() => {
    const q = (sel) => document.querySelector(sel);
    const btn = (id) => {
      const el = document.getElementById(id);
      if (!el) return `${id}:absent`;
      return `${id}:${el.disabled ? 'disabled' : 'enabled'}`;
    };
    return {
      pill: q('.game-header span')?.textContent ?? '(none)',
      buttons: [btn('bj-place-bet'), btn('bj-hit'), btn('bj-stand')],
      cardNodes: document.querySelectorAll('.blackjack-table .playing-card').length,
      strips: [...document.querySelectorAll('.status-strip')].map((s) =>
        s.textContent.replace(/\s+/g, ' ').trim()
      ),
    };
  });
}

function printFacts(name, f) {
  console.log(
    `  [${name}] pill=${f.pill} cards=${f.cardNodes} ${f.buttons.join(' ')}`
  );
  for (const s of f.strips) console.log(`  [${name}] strip: ${s}`);
}

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const { chromium } = require(path.join(FRONTEND, 'node_modules', 'playwright'));
  const setup = (await import(setupUrl)).default;
  const teardown = (await import(teardownUrl)).default;

  await setup();
  const browser = await chromium.launch();
  const netLog = [];
  try {
    const ctxA = await browser.newContext();
    const pageA = await ctxA.newPage();
    attachNetLog(pageA, 'A', netLog);
    const ctxB = await browser.newContext();
    const pageB = await ctxB.newPage();
    attachNetLog(pageB, 'B', netLog);

    // ---- setup: A creates a multiplayer blackjack room, B joins ------------
    const roomName = `Diag Room ${now()}`;
    await pageA.goto('http://127.0.0.1:4179/');
    await pageA.getByRole('button', { name: 'Play Blackjack' }).click();
    await pageA.getByRole('button', { name: '+ Create Room' }).waitFor({ timeout: 15000 });
    await pageA.getByRole('button', { name: '+ Create Room' }).click();
    await pageA.getByLabel('Room Name').fill(roomName);
    await pageA.locator('form button[type="submit"]').click();
    await pageA.getByText('Get Ready', { exact: true }).waitFor({ timeout: 15000 });

    const roomId = await pageA.evaluate(() => Object.keys(JSON.parse(sessionStorage.roomTokens))[0]);
    console.log(`roomId = ${roomId}`);

    await pageB.goto('http://127.0.0.1:4179/');
    await pageB.getByRole('button', { name: 'Play Blackjack' }).click();
    const row = pageB.locator(`div:has(> strong:text-is("${roomName}"))`);
    await row.waitFor({ timeout: 20000 });
    await row.locator('..').getByRole('button', { name: 'Join' }).click();
    await pageB.getByText('Get Ready', { exact: true }).waitFor({ timeout: 15000 });

    // ---- D3: phase transitions -------------------------------------------
    console.log('\n=== D3 (a) BEFORE any ready ===');
    let snap = await snapshot(roomId, 'S0 before-any-ready', null);

    console.log('\n=== D3 (b) waiting 6.5s with nobody ready (scheduler check) ===');
    await new Promise((r) => setTimeout(r, 6500));
    const snap2 = await snapshot(roomId, 'S0b +6.5s nobody ready', snap);
    if (snap2.st.roomPhase !== 'LOBBY') {
      console.log('  FINDING: room left LOBBY without any ready call:', snap2.st.roomPhase);
    } else {
      console.log('  confirmed: still LOBBY after 6.5s idle - scheduler never advances LOBBY');
    }
    snap = snap2;

    console.log('\n=== D3 (c) after player A readies ===');
    await pageA.getByRole('button', { name: "I'm Ready!" }).click();
    await pageA.getByText('Waiting for players...', { exact: true }).waitFor({ timeout: 10000 });
    snap = await snapshot(roomId, 'S1 after-A-ready', snap);

    console.log('\n=== D3 (d) after player B readies (room starts) ===');
    await pageB.getByRole('button', { name: "I'm Ready!" }).click();
    const s2immediate = await snapshot(roomId, 'S2 immediate after-B-ready', snap);
    await pageA.locator('.blackjack-table').waitFor({ timeout: 15000 });
    await pageB.locator('.blackjack-table').waitFor({ timeout: 15000 });
    console.log('  both game views visible');
    await new Promise((r) => setTimeout(r, 1500));
    const s2settled = await snapshot(roomId, 'S2b +1.5s after handoff', s2immediate);
    snap = s2settled;

    // ---- D4: screen facts before any bet ---------------------------------
    console.log('\n=== D4 (a) screens right after handoff (before any bet) ===');
    let fA = await screenFacts(pageA);
    let fB = await screenFacts(pageB);
    printFacts('A', fA);
    printFacts('B', fB);

    // ---- D2/D4: both players bet, go mid-round ----------------------------
    console.log('\n=== betting: A then B ===');
    await pageA.locator('#bj-bet-amount').fill('10');
    await pageA.locator('#bj-place-bet').click();
    await pageA.waitForFunction(
      () => document.querySelector('.game-header span')?.textContent === 'PLAYER_TURN',
      { timeout: 10000 }
    );
    console.log('  A reached PLAYER_TURN');
    printFacts('A after own bet', await screenFacts(pageA));
    printFacts('B before own bet (A already bet)', await screenFacts(pageB));

    await pageB.locator('#bj-bet-amount').fill('10');
    await pageB.locator('#bj-place-bet').click();
    await pageB.waitForFunction(
      () => document.querySelector('.game-header span')?.textContent === 'PLAYER_TURN',
      { timeout: 10000 }
    );
    console.log('  B reached PLAYER_TURN');

    // wait until both cards visible in both views
    for (let i = 0; i < 20; i++) {
      fA = await screenFacts(pageA);
      fB = await screenFacts(pageB);
      if (fA.cardNodes >= 4 && fB.cardNodes >= 4) break;
      await new Promise((r) => setTimeout(r, 250));
    }

    console.log('\n=== D4 (b) screens with both players mid-round ===');
    fA = await screenFacts(pageA);
    fB = await screenFacts(pageB);
    printFacts('A', fA);
    printFacts('B', fB);

    // ---- D2: verbatim mid-round dumps ------------------------------------
    console.log('\n=== D2: mid-round room state dump (both players 2+ cards) ===');
    const mid = await snapshot(roomId, 'S3 mid-round both-PLAYER_TURN', snap);
    fs.writeFileSync(path.join(OUT_DIR, 'state-mid.json'), JSON.stringify(mid.st, null, 2));
    fs.writeFileSync(path.join(OUT_DIR, 'progress-mid.json'), JSON.stringify(mid.pr, null, 2));
    console.log(`  (written to ${OUT_DIR}\\state-mid.json and progress-mid.json)`);

    const mid2 = await api(`/api/rooms/${roomId}/state`);
    const mid3 = await api(`/api/rooms/${roomId}/progress`);
    console.log('\n=== D2 curl-equivalent: GET /state ===');
    console.log(JSON.stringify(mid2.body, null, 2));
    console.log('=== D2 curl-equivalent: GET /progress ===');
    console.log(JSON.stringify(mid3.body, null, 2));

    // ---- D4: full network traces -----------------------------------------
    const fmt = (e) =>
      `  t+${String(e.t - netLog[0].t).padStart(6)}ms [${e.label}] ${e.method.padEnd(6)} ${e.url} -> ${e.status} (${e.ms}ms)`;
    console.log('\n=== D4: full /api/ network trace - player B (creator A first, then B) ===');
    for (const e of netLog.filter((e) => e.label === 'B').sort((a, b) => a.t - b.t)) {
      console.log(fmt(e));
    }
    console.log('\n=== D4: full /api/ network trace - player A ===');
    for (const e of netLog.filter((e) => e.label === 'A').sort((a, b) => a.t - b.t)) {
      console.log(fmt(e));
    }

    const bSession = netLog.find(
      (e) => e.label === 'B' && e.method === 'POST' && e.url === '/api/blackjack/sessions'
    );
    const bRegister = netLog.find(
      (e) => e.label === 'B' && e.method === 'POST' && e.url.includes('/api/rooms/') && e.url.endsWith('/sessions')
    );
    console.log('\n=== D4: session existence for B ===');
    console.log(
      `  B POST /api/blackjack/sessions: ${bSession ? `t+${bSession.t - netLog[0].t}ms status=${bSession.status}` : 'NEVER'}`
    );
    console.log(
      `  B POST /api/rooms/{id}/sessions (register): ${bRegister ? `t+${bRegister.t - netLog[0].t}ms status=${bRegister.status}` : 'NEVER'}`
    );

    await ctxA.close();
    await ctxB.close();
  } finally {
    await browser.close();
    await teardown();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
