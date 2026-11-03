const { test, expect } = require('@playwright/test');
const { authenticatedApiRequest, signInAsAdmin } = require('./helpers');

test('paginate student records and change the visible page size', async ({ page }) => {
  test.skip(!process.env.E2E_ADMIN_USERNAME || !process.env.E2E_ADMIN_PASSWORD, 'Set E2E_ADMIN_USERNAME and E2E_ADMIN_PASSWORD to run authenticated roster tests.');
  const suffix = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const ids = [];

  try {
    await signInAsAdmin(page);
    for (let index = 0; index < 6; index += 1) {
      const response = await authenticatedApiRequest(page, 'POST', '/api/students', {
          name: `Pagination ${suffix} ${index}`,
          email: `sms-pagination-${suffix}-${index}@example.test`,
          phone: '+1 555 123 4567',
          department: 'Engineering',
          course: 'Computer Science',
          year: 2,
      });
      expect(response.status).toBe(201);
      ids.push(response.body.id);
    }

    await page.getByRole('button', { name: 'Refresh' }).click();
    await page.getByRole('searchbox', { name: 'Search students' }).fill(`Pagination ${suffix}`);
    const rows = page.locator('#studentTableBody tr');
    await expect(rows).toHaveCount(5);

    await page.getByRole('button', { name: 'Next' }).click();
    await expect(page.getByText('Page 2')).toBeVisible();
    await expect(rows).toHaveCount(1);

    await page.getByRole('button', { name: 'Previous' }).click();
    await expect(page.getByText('Page 1')).toBeVisible();
    await expect(rows).toHaveCount(5);

    await page.getByLabel('Show').selectOption('10');
    await expect(rows).toHaveCount(6);
  } finally {
    for (const id of ids) {
      await authenticatedApiRequest(page, 'DELETE', `/api/students/${id}`);
    }
  }
});
