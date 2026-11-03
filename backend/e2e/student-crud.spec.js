const { test, expect } = require('@playwright/test');
const { addStudentThroughUi, fillStudentForm, uniqueStudent } = require('./helpers');

test('register, view, edit, and confirmed-delete a student', async ({ page, request }) => {
  const student = uniqueStudent('CRUD Student');
  let studentId;

  try {
    await page.goto('/');
    const row = await addStudentThroughUi(page, student);
    studentId = (await row.locator('td').first().innerText()).trim();

    await row.getByRole('button', { name: 'Details' }).click();
    const details = page.locator('#studentDetailsDialog');
    await expect(details).toBeVisible();
    await expect(details).toContainText(student.email);
    await expect(details).toContainText(student.dateOfBirth);
    await page.getByRole('button', { name: 'Close student details' }).click();

    await row.getByRole('button', { name: 'Edit' }).click();
    const updatedName = `${student.name} Updated`;
    student.name = updatedName;
    await fillStudentForm(page, student);
    await page.getByRole('button', { name: 'Save Student' }).click();
    await page.getByText('Student updated successfully.').waitFor();
    await expect(page.locator('#studentTableBody tr').filter({ hasText: student.email })).toContainText(updatedName);

    const updatedRow = page.locator('#studentTableBody tr').filter({ hasText: student.email });
    page.once('dialog', (dialog) => dialog.accept());
    await updatedRow.getByRole('button', { name: 'Delete' }).click();
    await expect(updatedRow).toHaveCount(0);
    await expect(page.getByText('Student deleted successfully.')).toBeVisible();
    studentId = undefined;
  } finally {
    if (studentId) {
      await request.delete(`/api/students/${studentId}`);
    }
  }
});
