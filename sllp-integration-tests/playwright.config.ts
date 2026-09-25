import { defineConfig } from '@playwright/test';

// API-only black-box integration tests — no browser is launched.
// They hit the api-gateway of a fully deployed stack (docker-compose),
// so GATEWAY_BASE_URL must point at a running environment.
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],
  use: {
    baseURL: process.env.GATEWAY_BASE_URL ?? 'http://localhost:8080',
  },
});
