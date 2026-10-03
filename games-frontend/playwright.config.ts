import { defineConfig } from '@playwright/test';

// STEP 6b Part B - real-browser scenarios B1-B6.
// One worker, no parallelism: all tests share one backend and B4 restarts it.
export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.mjs',
  globalTeardown: './e2e/global-teardown.mjs',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['list'],
    ['html', { outputFolder: 'e2e-report', open: 'never' }],
  ],
  use: {
    baseURL: 'http://127.0.0.1:4179',
    headless: true,
    trace: 'retain-on-failure',
    video: 'off',
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
});
