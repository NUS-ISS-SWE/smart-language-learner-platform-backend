import { test, expect } from '@playwright/test';
import { SEEDED_USERS, login, bearer } from './support/auth';

test.describe('[Integration] Subscription plans', () => {
  test('public plan list is available without authentication', async ({ request }) => {
    const response = await request.get('/api/v1/subscription/plans');
    expect(response.status()).toBe(200);

    const plans = await response.json();
    const codes = plans.map((plan: { code: string }) => plan.code);
    expect(codes).toEqual(expect.arrayContaining(['FREE_TRIAL', 'BASIC', 'PREMIUM']));
  });

  test('select, view and cancel the free plan', async ({ request }) => {
    const token = await login(request, SEEDED_USERS.testUser.username, SEEDED_USERS.testUser.password);
    const headers = bearer(token);

    await test.step('select FREE_TRIAL', async () => {
      const response = await request.post('/api/v1/subscription/select', {
        headers,
        data: { planCode: 'FREE_TRIAL' },
      });
      expect(response.status(), await response.text()).toBe(200);

      const subscription = await response.json();
      expect(subscription.status).toBe('ACTIVE');
      expect(subscription.plan.code).toBe('FREE_TRIAL');
    });

    await test.step('current subscription reflects the selection', async () => {
      const response = await request.get('/api/v1/subscription/current', { headers });
      expect(response.status()).toBe(200);

      const subscription = await response.json();
      expect(subscription.plan.code).toBe('FREE_TRIAL');
      expect(subscription.status).toBe('ACTIVE');
    });

    await test.step('history includes the new subscription', async () => {
      const response = await request.get('/api/v1/subscription/history', { headers });
      expect(response.status()).toBe(200);

      const history = await response.json();
      expect(history.length).toBeGreaterThan(0);
    });

    await test.step('cancel downgrades the subscription', async () => {
      const response = await request.post('/api/v1/subscription/cancel', { headers });
      expect(response.status(), await response.text()).toBe(200);

      const subscription = await response.json();
      expect(subscription.status).toBe('CANCELLED');
    });
  });
});
