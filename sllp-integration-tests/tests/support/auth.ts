import { APIRequestContext, APIResponse, expect } from '@playwright/test';

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
  await expectStatus(response, 200, `login as "${username}"`);
  const body = await response.json();
  return body.accessToken as string;
}

// The response body only enters the failure message, never the passing step's title,
// so tokens and payloads don't leak into the HTML report.
export async function expectStatus(response: APIResponse, expected: number, label: string): Promise<void> {
  if (response.status() !== expected) {
    const body = await response.text();
    expect(response.status(), `${label}: expected ${expected}, got ${response.status()} ${body}`).toBe(expected);
  }
}

export function bearer(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` };
}
