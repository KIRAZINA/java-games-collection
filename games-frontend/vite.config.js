import { defineConfig } from 'vite';
import { configDefaults } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    // The Playwright suite lives in e2e/ and is run by `npm run e2e`;
    // vitest's default include would collect e2e/step6b.spec.ts and fail
    // with "Playwright Test did not expect test() to be called here".
    exclude: [...configDefaults.exclude, 'e2e/**'],
    coverage: {
      reporter: ['text', 'html'],
    },
  },
});
