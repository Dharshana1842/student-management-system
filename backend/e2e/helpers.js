function uniqueStudent(label) {
  const suffix = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return {
    name: `${label} ${suffix}`,
    email: `sms-e2e-${suffix}@example.test`,
    phone: '+1 555 123 4567',
    department: 'Engineering',
    course: 'Computer Science',
    year: '2',
    dateOfBirth: '2004-01-15',
    address: 'E2E test address',
    admissionDate: '2025-09-01',
  };
}

async function fillStudentForm(page, student) {
  await page.getByLabel('Name', { exact: true }).fill(student.name);
  await page.getByLabel('Email', { exact: true }).fill(student.email);
  await page.getByLabel('Phone', { exact: true }).fill(student.phone);
  await page.getByLabel('Department', { exact: true }).fill(student.department);
  await page.getByLabel('Course', { exact: true }).fill(student.course);
  await page.getByLabel('Year', { exact: true }).fill(student.year);
  await page.getByLabel('Date of Birth', { exact: true }).fill(student.dateOfBirth);
  await page.getByLabel('Admission Date', { exact: true }).fill(student.admissionDate);
  await page.getByLabel('Address', { exact: true }).fill(student.address);
}

async function addStudentThroughUi(page, student) {
  await page.getByRole('link', { name: 'Add Student' }).click();
  await fillStudentForm(page, student);
  await page.getByRole('button', { name: 'Save Student' }).click();
  await page.getByText('Student added successfully.').waitFor();
  await page.getByRole('searchbox', { name: 'Search students' }).fill(student.email);
  const row = page.locator('#studentTableBody tr').filter({ hasText: student.email });
  await row.waitFor({ state: 'visible' });
  return row;
}

module.exports = { addStudentThroughUi, fillStudentForm, uniqueStudent };
