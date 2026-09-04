const API_URL = 'https://student-management-system-production-0e75.up.railway.app/api/students';

const studentForm = document.getElementById('studentForm');
const studentTableBody = document.getElementById('studentTableBody');
const studentIdInput = document.getElementById('studentId');
const formTitle = document.getElementById('formTitle');
const cancelEditButton = document.getElementById('cancelEdit');
const refreshButton = document.getElementById('refreshButton');
const clearFormButton = document.getElementById('clearFormBtn');
const searchInput = document.getElementById('searchInput');
const departmentFilter = document.getElementById('departmentFilter');
const courseFilter = document.getElementById('courseFilter');
const yearFilter = document.getElementById('yearFilter');
const clearFiltersBtn = document.getElementById('clearFiltersBtn');
const pageSizeSelect = document.getElementById('pageSizeSelect');
const prevPageButton = document.getElementById('prevPageButton');
const nextPageButton = document.getElementById('nextPageButton');
const currentPageLabel = document.getElementById('currentPageLabel');
const paginationSummary = document.getElementById('paginationSummary');
const editModeBadge = document.getElementById('editModeBadge');
const loadingOverlay = document.getElementById('loadingOverlay');
const toastContainer = document.getElementById('toastContainer');
const formErrorBox = document.getElementById('formErrorBox');

const state = {
  students: [],
  page: 1,
  pageSize: 5,
  search: '',
  filters: {
    department: '',
    course: '',
    year: '',
  },
};

function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;

  toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 3000);
}

function setLoading(isLoading, message = 'Loading students...') {
  if (isLoading) {
    loadingOverlay.classList.remove('hidden');
    loadingOverlay.querySelector('span').textContent = message;
    return;
  }

  loadingOverlay.classList.add('hidden');
}

function formatDate(value) {
  if (!value) {
    return '';
  }

  if (typeof value === 'string') {
    return value.split('T')[0];
  }

  return value;
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function getFormFieldValue(fieldId) {
  const element = document.getElementById(fieldId);
  return element ? element.value : '';
}

function normalizeStudentPayload() {
  const yearRaw = getFormFieldValue('year');
  const parsedYear = yearRaw === '' ? '' : Number(yearRaw);

  return {
    name: getFormFieldValue('name').trim(),
    email: getFormFieldValue('email').trim(),
    phone: getFormFieldValue('phone').trim(),
    department: getFormFieldValue('department').trim(),
    course: getFormFieldValue('course').trim(),
    year: parsedYear,
    dateOfBirth: getFormFieldValue('dateOfBirth') || null,
    address: getFormFieldValue('address').trim(),
    admissionDate: getFormFieldValue('admissionDate') || null,
  };
}

function resetForm() {
  studentForm.reset();
  studentIdInput.value = '';
  formTitle.textContent = 'Add Student';
  editModeBadge.classList.add('hidden');
  cancelEditButton.classList.add('hidden');
  clearFormErrors();
}

function clearFormErrors() {
  formErrorBox.classList.add('hidden');
  formErrorBox.textContent = '';

  document.querySelectorAll('.error-text').forEach((node) => {
    node.textContent = '';
  });
}

function showFormErrors(errors) {
  clearFormErrors();

  if (!Object.keys(errors).length) {
    return;
  }

  const messages = Object.values(errors);
  formErrorBox.textContent = messages[0];
  formErrorBox.classList.remove('hidden');

  Object.entries(errors).forEach(([field, message]) => {
    const errorNode = document.querySelector(`[data-error-for="${field}"]`);
    if (errorNode) {
      errorNode.textContent = message;
    }
  });
}

function validateStudent(student) {
  const errors = {};
  const normalizedCourse = typeof student.course === 'string' ? student.course.trim() : '';
  const normalizedYear = student.year === '' || student.year === null || student.year === undefined ? '' : Number(student.year);

  if (!student.name || !student.name.trim()) {
    errors.name = 'Name is required.';
  }

  if (!student.email || !student.email.trim()) {
    errors.email = 'Email is required.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(student.email)) {
    errors.email = 'Please enter a valid email address.';
  }

  if (!student.phone || !student.phone.trim()) {
    errors.phone = 'Phone is required.';
  } else if (!/^\+?[0-9\s()-]{7,20}$/.test(student.phone)) {
    errors.phone = 'Please enter a valid phone number.';
  }

  if (!student.department || !student.department.trim()) {
    errors.department = 'Department is required.';
  }

  if (!normalizedCourse) {
    errors.course = 'Course is required.';
  }

  if (normalizedYear === '' || normalizedYear === null || normalizedYear === undefined) {
    errors.year = 'Year is required.';
  } else if (!Number.isInteger(normalizedYear) || normalizedYear < 1 || normalizedYear > 10) {
    errors.year = 'Year must be a valid student year between 1 and 10.';
  }

  return errors;
}

function getDashboardStats() {
  const totalStudents = state.students.length;
  const currentYear = new Date().getFullYear();
  const currentYearCount = state.students.filter((student) => {
    if (!student.admissionDate) {
      return false;
    }

    const admissionDate = new Date(student.admissionDate);
    return !Number.isNaN(admissionDate.getTime()) && admissionDate.getFullYear() === currentYear;
  }).length;

  const departments = new Set(
    state.students
      .map((student) => student.department)
      .filter((department) => department && department.trim() !== '')
      .map((department) => department.trim().toLowerCase())
  );

  const recentThreshold = new Date();
  recentThreshold.setDate(recentThreshold.getDate() - 30);

  const recentStudents = state.students.filter((student) => {
    if (!student.admissionDate) {
      return false;
    }

    const dateValue = new Date(student.admissionDate);
    return !Number.isNaN(dateValue.getTime()) && dateValue >= recentThreshold;
  }).length;

  document.getElementById('totalStudentsStat').textContent = totalStudents;
  document.getElementById('currentYearStat').textContent = currentYearCount;
  document.getElementById('departmentStat').textContent = departments.size;
  document.getElementById('recentStudentsStat').textContent = recentStudents;
}

function buildFilterOptions() {
  const departments = [...new Set(state.students.map((student) => student.department).filter(Boolean))].sort();
  const courses = [...new Set(state.students.map((student) => student.course).filter(Boolean))].sort();
  const years = [...new Set(state.students.map((student) => student.year).filter((year) => year !== null && year !== undefined && year !== ''))].sort((a, b) => a - b);

  departmentFilter.innerHTML = '<option value="">All Departments</option>' + departments.map((department) => `<option value="${escapeHtml(department)}">${escapeHtml(department)}</option>`).join('');
  courseFilter.innerHTML = '<option value="">All Courses</option>' + courses.map((course) => `<option value="${escapeHtml(course)}">${escapeHtml(course)}</option>`).join('');
  yearFilter.innerHTML = '<option value="">All Years</option>' + years.map((year) => `<option value="${year}">${year}</option>`).join('');

  departmentFilter.value = state.filters.department || '';
  courseFilter.value = state.filters.course || '';
  yearFilter.value = state.filters.year || '';
}

function applyFilters() {
  const searchTerm = state.search.trim().toLowerCase();

  return state.students.filter((student) => {
    const matchesSearch = !searchTerm || [student.name, student.email, student.phone, student.department, student.course].some((value) => (value || '').toLowerCase().includes(searchTerm));
    const matchesDepartment = !state.filters.department || (student.department || '').toLowerCase() === state.filters.department.toLowerCase();
    const matchesCourse = !state.filters.course || (student.course || '').toLowerCase() === state.filters.course.toLowerCase();
    const matchesYear = !state.filters.year || String(student.year) === String(state.filters.year);

    return matchesSearch && matchesDepartment && matchesCourse && matchesYear;
  });
}

function renderStudents() {
  const filteredStudents = applyFilters();
  const totalPages = Math.max(1, Math.ceil(filteredStudents.length / state.pageSize));

  if (state.page > totalPages) {
    state.page = totalPages;
  }

  const startIndex = (state.page - 1) * state.pageSize;
  const endIndex = startIndex + state.pageSize;
  const visibleStudents = filteredStudents.slice(startIndex, endIndex);

  prevPageButton.disabled = state.page <= 1;
  nextPageButton.disabled = state.page >= totalPages;
  currentPageLabel.textContent = `Page ${state.page}`;
  paginationSummary.textContent = filteredStudents.length ? `Showing ${Math.min(startIndex + 1, filteredStudents.length)}-${Math.min(endIndex, filteredStudents.length)} of ${filteredStudents.length} students` : 'Showing 0-0 of 0 students';

  if (!filteredStudents.length) {
    studentTableBody.innerHTML = `
      <tr>
        <td colspan="11" class="empty-state-column">
          <div class="empty-state">
            <h3>No students found</h3>
            <p>Start by adding your first student.</p>
            <button type="button" class="btn btn-primary" id="addFirstStudentBtn">Add Your First Student</button>
          </div>
        </td>
      </tr>
    `;

    const addFirstStudentBtn = document.getElementById('addFirstStudentBtn');
    if (addFirstStudentBtn) {
      addFirstStudentBtn.addEventListener('click', () => {
        resetForm();
        document.getElementById('add-student').scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    }

    return;
  }

  studentTableBody.innerHTML = visibleStudents
    .map((student) => {
      const initials = (student.name || 'S')
        .split(' ')
        .map((part) => part[0])
        .join('')
        .slice(0, 2)
        .toUpperCase();

      return `
        <tr>
          <td>${student.id ?? ''}</td>
          <td>
            <div class="name-cell">
              <div class="avatar">${escapeHtml(initials)}</div>
              <span>${escapeHtml(student.name || '')}</span>
            </div>
          </td>
          <td>${escapeHtml(student.email || '')}</td>
          <td>${escapeHtml(student.phone || '')}</td>
          <td>${escapeHtml(student.department || '')}</td>
          <td>
            <div class="badge-list">
              <span class="info-badge">${escapeHtml(student.course || '')}</span>
            </div>
          </td>
          <td>
            <div class="badge-list">
              <span class="info-badge">${escapeHtml(student.year ?? '')}</span>
            </div>
          </td>
          <td>${formatDate(student.dateOfBirth)}</td>
          <td>${formatDate(student.admissionDate)}</td>
          <td>${escapeHtml(student.address || '')}</td>
          <td class="actions-cell">
            <div class="action-group">
              <button class="action-btn edit" type="button" data-id="${student.id}">Edit</button>
              <button class="action-btn delete" type="button" data-id="${student.id}">Delete</button>
            </div>
          </td>
        </tr>
      `;
    })
    .join('');
}

async function fetchStudents() {
  setLoading(true, 'Loading students...');

  try {
    const response = await fetch(API_URL);

    if (!response.ok) {
      throw new Error(`Failed to fetch students: ${response.status}`);
    }

    state.students = await response.json();
    buildFilterOptions();
    getDashboardStats();
    renderStudents();
  } catch (error) {
    console.error(error);
    studentTableBody.innerHTML = `
      <tr>
        <td colspan="11" class="empty-state-column">
          <div class="empty-state">
            <h3>Unable to load students</h3>
            <p>Make sure the backend is running on port 8080.</p>
          </div>
        </td>
      </tr>
    `;
    showToast('Unable to load students from the API.', 'error');
  } finally {
    setLoading(false);
  }
}

function populateForm(student) {
  studentIdInput.value = student.id;
  document.getElementById('name').value = student.name || '';
  document.getElementById('email').value = student.email || '';
  document.getElementById('phone').value = student.phone || '';
  document.getElementById('department').value = student.department || '';
  document.getElementById('course').value = student.course || '';
  document.getElementById('year').value = student.year === null || student.year === undefined ? '' : String(student.year);
  document.getElementById('dateOfBirth').value = formatDate(student.dateOfBirth);
  document.getElementById('address').value = student.address || '';
  document.getElementById('admissionDate').value = formatDate(student.admissionDate);

  formTitle.textContent = 'Edit Student';
  editModeBadge.classList.remove('hidden');
  cancelEditButton.classList.remove('hidden');
  clearFormErrors();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function editStudent(studentId) {
  setLoading(true, 'Loading student details...');

  try {
    const response = await fetch(`${API_URL}/${studentId}`);

    if (!response.ok) {
      throw new Error('Failed to load student');
    }

    const student = await response.json();
    populateForm(student);
  } catch (error) {
    console.error(error);
    showToast('Unable to load the selected student.', 'error');
  } finally {
    setLoading(false);
  }
}

async function deleteStudent(studentId) {
  const confirmed = window.confirm('Are you sure you want to delete this student?');

  if (!confirmed) {
    return;
  }

  setLoading(true, 'Deleting student...');

  try {
    const response = await fetch(`${API_URL}/${studentId}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Delete failed');
    }

    if (studentIdInput.value === String(studentId)) {
      resetForm();
    }

    await fetchStudents();
    showToast('Student deleted successfully.', 'success');
  } catch (error) {
    console.error(error);
    showToast('Unable to delete student.', 'error');
  } finally {
    setLoading(false);
  }
}

async function handleFormSubmit(event) {
  event.preventDefault();

  const student = normalizeStudentPayload();
  const errors = validateStudent(student);

  if (Object.keys(errors).length) {
    showFormErrors(errors);
    showToast('Please correct the highlighted form errors.', 'error');
    return;
  }

  const studentId = studentIdInput.value;
  const url = studentId ? `${API_URL}/${studentId}` : API_URL;
  const method = studentId ? 'PUT' : 'POST';

  setLoading(true, studentId ? 'Saving student changes...' : 'Adding student...');

  try {
    const response = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(student),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Request failed');
    }

    resetForm();
    await fetchStudents();
    showToast(studentId ? 'Student updated successfully.' : 'Student added successfully.', 'success');
  } catch (error) {
    console.error(error);
    showToast('Unable to save student. Please check the backend and form values.', 'error');
  } finally {
    setLoading(false);
  }
}

function attachHandlers() {
  studentForm.addEventListener('submit', handleFormSubmit);
  refreshButton.addEventListener('click', fetchStudents);
  clearFormButton.addEventListener('click', resetForm);
  cancelEditButton.addEventListener('click', resetForm);
  clearFiltersBtn.addEventListener('click', () => {
    state.filters.department = '';
    state.filters.course = '';
    state.filters.year = '';
    searchInput.value = '';
    state.search = '';
    state.page = 1;
    departmentFilter.value = '';
    courseFilter.value = '';
    yearFilter.value = '';
    renderStudents();
  });

  searchInput.addEventListener('input', (event) => {
    state.search = event.target.value;
    state.page = 1;
    renderStudents();
  });

  departmentFilter.addEventListener('change', (event) => {
    state.filters.department = event.target.value;
    state.page = 1;
    renderStudents();
  });

  courseFilter.addEventListener('change', (event) => {
    state.filters.course = event.target.value;
    state.page = 1;
    renderStudents();
  });

  yearFilter.addEventListener('change', (event) => {
    state.filters.year = event.target.value;
    state.page = 1;
    renderStudents();
  });

  pageSizeSelect.addEventListener('change', (event) => {
    state.pageSize = Number(event.target.value);
    state.page = 1;
    renderStudents();
  });

  prevPageButton.addEventListener('click', () => {
    if (state.page > 1) {
      state.page -= 1;
      renderStudents();
    }
  });

  nextPageButton.addEventListener('click', () => {
    const filteredStudents = applyFilters();
    const totalPages = Math.max(1, Math.ceil(filteredStudents.length / state.pageSize));
    if (state.page < totalPages) {
      state.page += 1;
      renderStudents();
    }
  });

  studentTableBody.addEventListener('click', (event) => {
    const target = event.target;

    if (!(target instanceof HTMLElement)) {
      return;
    }

    const studentId = target.dataset.id;

    if (!studentId) {
      return;
    }

    if (target.classList.contains('edit')) {
      editStudent(studentId);
    }

    if (target.classList.contains('delete')) {
      deleteStudent(studentId);
    }
  });
}

attachHandlers();
fetchStudents();
