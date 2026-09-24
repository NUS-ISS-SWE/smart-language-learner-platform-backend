import { APIRequestContext, expect } from '@playwright/test';

// Seeded by sllp-liquibase-service (001-schema-init.sql, changeset seed-dev-users).
// Present in any environment that has run the Liquibase migrations.
export const SEEDED_USERS = {
  admin: { username: 'admin', password: 'password123' },
  testUser: { username: 'testuser', password: 'password123' },
} as const;

export async function login(request: APIRequestContext, username: string, password: string): Promise<string> {
  const response = await request.post('/api/v1/auth/login', {
    data: { username, password },
  });
  expect(response.ok(), `login failed for "${username}": ${response.status()} ${await response.text()}`).toBeTruthy();
  const body = await response.json();
  return body.accessToken as string;
}

export function bearer(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` };
}
