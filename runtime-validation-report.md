# Student Management System — Runtime Testing Report

**Result:** PASS  
**Test date:** 2026-11-03  
**Environment:** Java 25.0.5, Spring Boot backend, MySQL at `localhost:3306/student_management`, Playwright 1.64.1 with Chromium

## Results

| # | Requirement | Result | Evidence |
|---|---|---|---|
| 1 | Add a student | PASS | Browser test created a uniquely identified student through the registration form and confirmed the success notification and new table row. |
| 2 | Display student records | PASS | Browser tests loaded and displayed records returned by the running MySQL-backed application. |
| 3 | Search students | PASS | Browser test searched by name and student ID, then cleared filters and refreshed the list. |
| 4 | Edit student details | PASS | Browser test changed a student's name and verified the updated row and success notification. |
| 5 | Delete a student | PASS | Browser test accepted the confirmation dialog, verified the row disappeared, and confirmed the success notification. |
| 6 | MySQL connectivity | PASS | Application startup logged a successful Hikari connection using the MySQL JDBC URL. Browser CRUD tests then read and wrote records through that connection. |
| 7 | Records survive refresh | PASS | Browser test created a student, reloaded the page, found the record again, and fetched the same student by ID from the API with matching email and date of birth. |
| 8 | Frontend/backend communication | PASS | Chromium exercised the UI against the running backend; the persistence test also verified the corresponding REST response. |
| 9 | Errors and warnings | PASS | Maven tests completed with zero failures/errors and no warning output. All five browser tests passed. |
| 10 | Responsive layout | PASS | Browser test checked desktop at 1365×900 and mobile at 390×844; mobile document width did not overflow the viewport. |
| 11 | Buttons and navigation | PASS | Browser tests exercised Dashboard/Students/Add Student navigation, Clear, Cancel Edit, Refresh, search filters, pagination, page size, Details, Edit, Save, and confirmed Delete. |

## Commands and outcomes

- `.\mvnw.cmd test` using Java 25.0.5 — **BUILD SUCCESS**, 2 tests passed, 0 failures, 0 errors, 0 skipped.
- `npm run test:e2e` — **5 passed** using one worker against the live MySQL-backed application.
- `GET http://127.0.0.1:8080/api/students` after the browser run — API responded successfully; no records with the E2E fixture email prefixes remained.

The E2E suite uses unique records and deletes them after each test. No production application features were removed or replaced with dummy data for these checks.

## Notes

- Maven test execution was kept separate from the browser suite because both use the same build output; the final sequential runs passed.
- The Surefire test JVM uses Mockito's startup agent to avoid dynamic-agent warnings on Java 25. Class-data sharing is disabled for that test JVM to avoid its agent-related warning.
- This report covers local testing only; it does not claim that a public deployment or live website was verified.
