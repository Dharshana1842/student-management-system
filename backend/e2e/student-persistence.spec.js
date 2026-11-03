const { test, expect } = require('@playwright/test');
const { addStudentThroughUi, uniqueStudent } = require('./helpers');

test('persist student records after reloading the application', async ({ page, request }) => {
  const student = uniqueStudent('Persistent Student');
  let studentId;

  try {
    await page.goto('/');
    const row = await addStudentThroughUi(page, student);
    studentId = (await row.locator('td').first().innerText()).trim();

    await page.reload();
    await page.getByRole('searchbox', { name: 'Search students' }).fill(student.email);
    const persistedRow = page.locator('#studentTableBody tr').filter({ hasText: student.email });
    await expect(persistedRow).toBeVisible();

    const apiResponse = await request.get(`/api/students/${studentId}`);
    expect(apiResponse.ok()).toBeTruthy();
    const persistedStudent = await apiResponse.json();
    expect(persistedStudent.email).toBe(student.email);
    expect(persistedStudent.dateOfBirth).toBe(student.dateOfBirth);
  } finally {
    if (studentId) {
      await request.delete(`/api/students/${studentId}`);
    }
  }
});
