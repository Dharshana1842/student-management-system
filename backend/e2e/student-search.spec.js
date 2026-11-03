const { test, expect } = require('@playwright/test');
const { addStudentThroughUi, uniqueStudent } = require('./helpers');

test('search by name and student ID, then clear and refresh records', async ({ page, request }) => {
  const student = uniqueStudent('Search Student');
  let studentId;

  try {
    await page.goto('/');
    const row = await addStudentThroughUi(page, student);
    studentId = (await row.locator('td').first().innerText()).trim();
    const search = page.getByRole('searchbox', { name: 'Search students' });

    await search.fill(student.name);
    await expect(page.locator('#studentTableBody tr')).toHaveCount(1);
    await expect(page.locator('#studentTableBody tr').first()).toContainText(student.email);

    await search.fill(studentId);
    await expect(page.locator('#studentTableBody tr')).toHaveCount(1);
    await expect(page.locator('#studentTableBody tr').first().locator('td').first()).toHaveText(studentId);

    await page.getByRole('button', { name: 'Clear Filters' }).click();
    await expect(search).toHaveValue('');
    await page.getByRole('button', { name: 'Refresh' }).click();
    await search.fill(student.email);
    await expect(page.locator('#studentTableBody tr').filter({ hasText: student.email })).toBeVisible();
  } finally {
    if (studentId) {
      await request.delete(`/api/students/${studentId}`);
    }
  }
});
