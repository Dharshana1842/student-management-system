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

async function signInAsAdmin(page) {
  const username = process.env.E2E_ADMIN_USERNAME;
  const password = process.env.E2E_ADMIN_PASSWORD;
  if (!username || !password) {
    throw new Error('Set E2E_ADMIN_USERNAME and E2E_ADMIN_PASSWORD to run roster end-to-end tests.');
  }
  await page.goto('/');
  await page.getByLabel('Email or admin username').fill(username);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.locator('#appShell:not(.hidden)').waitFor();
}

async function authenticatedApiRequest(page, method, path, data) {
  return page.evaluate(async ({ method, path, data }) => {
    const csrfResponse = await fetch('/api/auth/csrf', { credentials: 'include' });
    const csrf = await csrfResponse.json();
    const response = await fetch(path, {
      method,
      credentials: 'include',
      headers: {
        'X-CSRF-TOKEN': csrf.token,
        ...(data === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      ...(data === undefined ? {} : { body: JSON.stringify(data) }),
    });
    const text = await response.text();
    return { status: response.status, body: text ? JSON.parse(text) : null };
  }, { method, path, data });
}

async function fillStudentForm(page, student) {
  const form = page.locator('#studentForm');
  await form.getByLabel('Name', { exact: true }).fill(student.name);
  await form.getByLabel('Email', { exact: true }).fill(student.email);
  await form.getByLabel('Phone', { exact: true }).fill(student.phone);
  await form.getByLabel('Department', { exact: true }).fill(student.department);
  await form.getByLabel('Course', { exact: true }).fill(student.course);
  await form.getByLabel('Year', { exact: true }).fill(student.year);
  await form.getByLabel('Date of Birth', { exact: true }).fill(student.dateOfBirth);
  await form.getByLabel('Admission Date', { exact: true }).fill(student.admissionDate);
  await form.getByLabel('Address', { exact: true }).fill(student.address);
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

module.exports = {
  addStudentThroughUi,
  authenticatedApiRequest,
  fillStudentForm,
  signInAsAdmin,
  uniqueStudent,
};
