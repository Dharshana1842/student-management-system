const { test, expect } = require('@playwright/test');

test('requires a session and exposes the roster-bound student registration flow', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
  await expect(page.locator('#appShell')).toBeHidden();

  await page.getByRole('button', { name: 'Register as a student' }).click();
  await expect(page.getByRole('heading', { name: 'Create a student account' })).toBeVisible();
  await expect(page.getByLabel('Student ID')).toBeVisible();
  await expect(page.getByLabel('Roster email')).toBeVisible();
  await expect(page.getByLabel('Date of birth on roster')).toBeVisible();
  await expect(page.getByText(/existing roster entry/)).toBeVisible();

  await page.getByRole('button', { name: 'Back to sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
  await expect(page.locator('#appShell')).toBeHidden();
});

test('rejects invalid credentials without revealing or opening the application', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Email or admin username').fill('unknown@example.test');
  await page.getByLabel('Password', { exact: true }).fill('not-a-valid-password');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page.getByRole('alert')).toHaveText('Sign-in failed. Check your credentials and try again.');
  await expect(page.locator('#appShell')).toBeHidden();
});
