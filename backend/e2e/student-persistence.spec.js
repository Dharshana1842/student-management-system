const { test, expect } = require('@playwright/test');
const { addStudentThroughUi, authenticatedApiRequest, signInAsAdmin, uniqueStudent } = require('./helpers');

test('persist student records after reloading the application', async ({ page }) => {
  test.skip(!process.env.E2E_ADMIN_USERNAME || !process.env.E2E_ADMIN_PASSWORD, 'Set E2E_ADMIN_USERNAME and E2E_ADMIN_PASSWORD to run authenticated roster tests.');
  const student = uniqueStudent('Persistent Student');
  let studentId;

  try {
    await signInAsAdmin(page);
    const row = await addStudentThroughUi(page, student);
    studentId = (await row.locator('td').first().innerText()).trim();

    await page.reload();
    await page.getByRole('searchbox', { name: 'Search students' }).fill(student.email);
    const persistedRow = page.locator('#studentTableBody tr').filter({ hasText: student.email });
    await expect(persistedRow).toBeVisible();

    const apiResponse = await authenticatedApiRequest(page, 'GET', `/api/students/${studentId}`);
    expect(apiResponse.status).toBe(200);
    const persistedStudent = apiResponse.body;
    expect(persistedStudent.email).toBe(student.email);
    expect(persistedStudent.dateOfBirth).toBe(student.dateOfBirth);
  } finally {
    if (studentId) {
      await authenticatedApiRequest(page, 'DELETE', `/api/students/${studentId}`);
    }
  }
});
