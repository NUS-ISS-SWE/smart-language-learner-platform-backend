import { test, expect } from '@playwright/test';
import { SEEDED_USERS, login, bearer } from './support/auth';

test.describe('Admin rubric criteria', () => {
  test('create, list, get, update and delete a rubric criterion', async ({ request }) => {
    const token = await login(request, SEEDED_USERS.admin.username, SEEDED_USERS.admin.password);
    const headers = bearer(token);
    let criterionId: number;

    await test.step('create a criterion', async () => {
      const response = await request.post('/api/v1/admin/rubrics', {
        headers,
        data: {
          language: 'ENGLISH',
          difficultyLevel: 'BEGINNER',
          criterionName: `Playwright IT ${Date.now()}`,
          criterionDescription: 'Created by Playwright integration test',
          weightPercentage: 10.5,
        },
      });
      expect(response.status(), await response.text()).toBe(201);

      const created = await response.json();
      expect(created.id).toBeDefined();
      criterionId = created.id;
    });

    await test.step('appears in the filtered list', async () => {
      const response = await request.get('/api/v1/admin/rubrics?language=ENGLISH&difficultyLevel=BEGINNER', { headers });
      expect(response.status()).toBe(200);

      const list = await response.json();
      expect(list.some((criterion: { id: number }) => criterion.id === criterionId)).toBeTruthy();
    });

    await test.step('get by id returns the criterion', async () => {
      const response = await request.get(`/api/v1/admin/rubrics/${criterionId}`, { headers });
      expect(response.status()).toBe(200);

      const criterion = await response.json();
      expect(criterion.id).toBe(criterionId);
      expect(criterion.criterionDescription).toBe('Created by Playwright integration test');
    });

    await test.step('update changes the weight', async () => {
      const response = await request.put(`/api/v1/admin/rubrics/${criterionId}`, {
        headers,
        data: {
          language: 'ENGLISH',
          difficultyLevel: 'BEGINNER',
          criterionName: `Playwright IT ${criterionId} (updated)`,
          criterionDescription: 'Updated by Playwright integration test',
          weightPercentage: 20,
        },
      });
      expect(response.status(), await response.text()).toBe(200);

      const updated = await response.json();
      expect(Number(updated.weightPercentage)).toBe(20);
    });

    await test.step('delete removes the criterion', async () => {
      const response = await request.delete(`/api/v1/admin/rubrics/${criterionId}`, { headers });
      expect(response.status()).toBe(204);
    });

    await test.step('get by id now returns 404', async () => {
      const response = await request.get(`/api/v1/admin/rubrics/${criterionId}`, { headers });
      expect(response.status()).toBe(404);
    });
  });
});
