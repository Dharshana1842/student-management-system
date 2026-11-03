const { test, expect } = require('@playwright/test');
const { addStudentThroughUi, uniqueStudent } = require('./helpers');

test('navigate between sections and use the form controls on desktop and mobile', async ({ page, request }) => {
  const student = uniqueStudent('Navigation Student');
  let studentId;

  try {
    await page.setViewportSize({ width: 1365, height: 900 });
    await page.goto('/');

    await page.getByRole('link', { name: 'Dashboard' }).click();
    await expect(page).toHaveURL(/#dashboard$/);
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

    await page.getByRole('link', { name: 'Students' }).click();
    await expect(page).toHaveURL(/#students$/);
    await expect(page.getByRole('heading', { name: 'Student List' })).toBeVisible();

    await page.getByRole('link', { name: 'Add Student' }).click();
    await expect(page).toHaveURL(/#add-student$/);
    await page.getByLabel('Name', { exact: true }).fill('Form control check');
    await page.getByRole('button', { name: 'Clear', exact: true }).click();
    await expect(page.getByLabel('Name', { exact: true })).toHaveValue('');

    const row = await addStudentThroughUi(page, student);
    studentId = (await row.locator('td').first().innerText()).trim();
    await row.getByRole('button', { name: 'Edit' }).click();
    await page.getByRole('button', { name: 'Cancel Edit' }).click();
    await expect(page.getByRole('heading', { name: 'Add Student' })).toBeVisible();
    await expect(row).toBeVisible();

    await page.setViewportSize({ width: 390, height: 844 });
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    const hasHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
    expect(hasHorizontalOverflow).toBe(false);
  } finally {
    if (studentId) {
      await request.delete(`/api/students/${studentId}`);
    }
  }
});
