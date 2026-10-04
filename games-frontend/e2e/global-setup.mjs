// Playwright globalSetup for STEP 6b B1-B6.
// 1) backend: reuse an already-healthy server on 8080, otherwise start one on
//    a random free port via bash scripts/e2e/start_server.sh (which builds the
//    jar if missing and waits for /actuator/health).
// 2) frontend: npm run build unless SKIP_BUILD=1.
// 3) dist: serve games-frontend/dist on the fixed test port 4179 (e2e/serve-dist.mjs)
//    proxying /api to the backend; reused if a healthy instance already answers
//    /__e2e-health for the same backend.
// Everything is recorded in a state file consumed by global-teardown; worker
// processes inherit the E2E_* env vars set here.
import { spawn, spawnSync } from 'node:child_process';
import { existsSync, writeFileSync, readFileSync, rmSync } from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import os from 'node:os';

const FRONTEND = process.cwd();
const ROOT = path.resolve(FRONTEND, '..');
const DIST_PORT = 4179;
const STATE_FILE = path.join(os.tmpdir(), 'step6b-e2e-state.json');

const log = (...a) => console.log('[global-setup]', ...a);

if (!existsSync(path.join(ROOT, 'scripts', 'e2e', 'start_server.sh'))) {
  throw new Error(`start_server.sh not found under ${ROOT} - run npm run e2e from games-frontend`);
}

function findBash() {
  const candidates =
    process.platform === 'win32'
      ? ['C:\\Program Files\\Git\\bin\\bash.exe', 'C:\\Program Files\\Git\\usr\\bin\\bash.exe', 'bash']
      : ['bash'];
  for (const cand of candidates) {
    try {
      const r = spawnSync(cand, ['--version'], { stdio: 'ignore' });
      if (r.status === 0) return cand;
    } catch {
      /* try next */
    }
  }
  throw new Error('bash not found - git-bash is required to run scripts/e2e/*.sh');
}

const bash = findBash();

function sh(script, env = {}) {
  return spawnSync(bash, ['-c', script], {
    cwd: ROOT,
    env: { ...process.env, ...env },
    encoding: 'utf8',
  });
}

async function backendHealthy(port) {
  try {
    const r = await fetch(`http://127.0.0.1:${port}/actuator/health`, {
      signal: AbortSignal.timeout(3000),
    });
    return r.ok;
  } catch {
    return false;
  }
}

function freePort() {
  return new Promise((resolve, reject) => {
    const srv = net.createServer();
    srv.on('error', reject);
    srv.listen(0, '127.0.0.1', () => {
      const p = srv.address().port;
      srv.close(() => resolve(p));
    });
  });
}

async function distHealth() {
  try {
    const r = await fetch('http://127.0.0.1:4179/__e2e-health', {
      signal: AbortSignal.timeout(1500),
    });
    if (!r.ok) return null;
    return await r.json();
  } catch {
    return null;
  }
}

function killPid(pid) {
  if (!pid) return;
  try {
    if (process.platform === 'win32') {
      spawnSync('taskkill', ['/F', '/PID', String(pid)], { stdio: 'ignore' });
    } else {
      process.kill(pid);
    }
  } catch {
    /* already gone */
  }
}

// ---- 1. backend ---------------------------------------------------------
// Playwright >= 1.4x requires the file to export a single function, so the
// whole sequence below runs inside the default export.
export default async function globalSetup() {
let backendPort;
let backendOwned;
if (await backendHealthy(8080)) {
  backendPort = 8080;
  backendOwned = false;
  log('reusing already-running backend on 8080');
} else {
  backendPort = await freePort();
  log(`starting backend on random port ${backendPort}`);
  const r = sh(`PORT=${backendPort} bash scripts/e2e/start_server.sh`);
  process.stdout.write(r.stdout || '');
  if (r.status !== 0) {
    process.stderr.write(r.stderr || '');
    throw new Error(`start_server.sh failed with status ${r.status}`);
  }
  backendOwned = true;
}
const backendUrl = `http://127.0.0.1:${backendPort}`;
if (!(await backendHealthy(backendPort))) {
  throw new Error(`backend not healthy at ${backendUrl}`);
}

// ---- 2. frontend build ---------------------------------------------------
if (process.env.SKIP_BUILD !== '1') {
  log('npm run build ...');
  // spawnSync('npm.cmd') is EINVAL on Node >= 18.20/20.12 on Windows (.cmd
  // launch requires shell) - run through cmd.exe explicitly.
  const b =
    process.platform === 'win32'
      ? spawnSync(process.env.ComSpec ?? 'cmd.exe', ['/d', '/s', '/c', 'npm.cmd run build'], {
          cwd: FRONTEND,
          stdio: 'inherit',
        })
      : spawnSync('npm', ['run', 'build'], { cwd: FRONTEND, stdio: 'inherit' });
  if (b.status !== 0) throw new Error('npm run build failed');
} else {
  log('SKIP_BUILD=1 - reusing existing dist');
}
if (!existsSync(path.join(FRONTEND, 'dist', 'index.html'))) {
  throw new Error('dist/index.html missing after build');
}

// ---- 3. dist server ------------------------------------------------------
// A leftover dist server from a crashed run points at a possibly-dead backend:
// kill it via the previous state file and start a fresh one.
const prev = existsSync(STATE_FILE) ? JSON.parse(readFileSync(STATE_FILE, 'utf8')) : null;
let health = await distHealth();
if (health && health.backend !== backendUrl) {
  log(`stale dist server targets ${health.backend}, killing pid ${prev?.distPid}`);
  killPid(prev?.distPid);
  await new Promise((r) => setTimeout(r, 500));
  health = await distHealth();
}

let distPid = null;
if (health) {
  log(`reusing dist server on ${DIST_PORT} (backend ${health.backend})`);
} else {
  const child = spawn(process.execPath, ['e2e/serve-dist.mjs'], {
    cwd: FRONTEND,
    env: {
      ...process.env,
      E2E_DIST_PORT: String(DIST_PORT),
      E2E_BACKEND_URL: backendUrl,
    },
    stdio: 'inherit',
  });
  distPid = child.pid;
  let ready = false;
  for (let i = 0; i < 80 && !ready; i++) {
    if (child.exitCode !== null) throw new Error(`serve-dist exited early with ${child.exitCode}`);
    ready = (await distHealth()) !== null;
    if (!ready) await new Promise((r) => setTimeout(r, 250));
  }
  if (!ready) throw new Error('serve-dist did not become healthy in 20s');
  log(`dist server pid=${distPid}`);
}

// ---- 4. end-to-end smoke -------------------------------------------------
const index = await fetch('http://127.0.0.1:4179/');
if (!index.ok) throw new Error(`dist server index.html status ${index.status}`);
const rooms = await fetch('http://127.0.0.1:4179/api/rooms');
if (!rooms.ok) throw new Error(`proxy /api/rooms status ${rooms.status}`);

// ---- 5. state + worker env ----------------------------------------------
process.env.E2E_BASH = bash;
process.env.E2E_BACKEND_PORT = String(backendPort);
process.env.E2E_BACKEND_OWNED = backendOwned ? '1' : '0';
process.env.E2E_BACKEND_URL = backendUrl;
writeFileSync(
  STATE_FILE,
  JSON.stringify({ bash, backendPort, backendOwned, backendUrl, distPid, distPort: DIST_PORT }, null, 2)
);
rmSync(path.join(FRONTEND, 'e2e-report'), { recursive: true, force: true });
log(`ready backend=${backendUrl} owned=${backendOwned} dist=:${DIST_PORT} pid=${distPid ?? 'reused'}`);
}
