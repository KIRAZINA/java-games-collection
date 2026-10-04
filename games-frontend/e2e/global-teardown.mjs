// Playwright globalTeardown for STEP 6b B1-B6.
// Stops the dist server started by globalSetup and, only if globalSetup
// started it (E2E_BACKEND_OWNED/state.backendOwned), the backend via
// scripts/e2e/stop_server.sh. A backend that was already running before the
// suite (reused on 8080) is left alone - except that B4 may have restarted
// it, which keeps it alive on the same port, preserving pre-run state.
import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync, rmSync } from 'node:fs';
import path from 'node:path';
import os from 'node:os';

const STATE_FILE = path.join(os.tmpdir(), 'step6b-e2e-state.json');
const log = (...a) => console.log('[global-teardown]', ...a);

export default function globalTeardown() {
if (!existsSync(STATE_FILE)) {
  log('no state file - nothing to tear down');
  return;
}
const state = JSON.parse(readFileSync(STATE_FILE, 'utf8'));

if (state.distPid) {
  try {
    if (process.platform === 'win32') {
      spawnSync('taskkill', ['/F', '/PID', String(state.distPid)], { stdio: 'ignore' });
    } else {
      process.kill(state.distPid);
    }
    log(`stopped dist server pid=${state.distPid}`);
  } catch (err) {
    log(`dist server pid=${state.distPid} already gone (${err.message})`);
  }
} else {
  log('dist server was reused - not stopping it');
}

if (state.backendOwned && state.bash) {
  const r = spawnSync(state.bash, ['-c', `PORT=${state.backendPort} bash scripts/e2e/stop_server.sh`], {
    cwd: path.resolve(process.cwd(), '..'),
    env: process.env,
    encoding: 'utf8',
  });
  process.stdout.write(r.stdout || '');
  if (r.status !== 0) process.stderr.write(r.stderr || '');
} else {
  log(`backend on ${state.backendPort} was reused - leaving it running`);
}

rmSync(STATE_FILE, { force: true });
log('done');
}
