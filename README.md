# Student Management System

A full-stack student records application with a responsive dashboard, a Spring Boot REST API, and MySQL persistence. The backend serves the existing HTML, CSS, and JavaScript frontend and validates every student create/update request.

## Features

- Dashboard statistics, searchable and filterable student list, and pagination.
- Register, view details, edit, and delete student records with confirmation.
- Client- and server-side validation, clear API errors, loading indicators, and notifications.
- Server-managed authenticated sessions, CSRF protection, admin/student roles, and roster-bound student registration.
- MySQL persistence with a versioned schema script.
- Responsive interface for desktop and mobile.

## Technologies

- Java 21 (LTS)
- Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Jakarta Validation
- MySQL 8+
- Maven Wrapper
- HTML5, CSS3, and vanilla JavaScript
- Capacitor 8 for the Android application

## Project structure

```text
StudentManagementSystem/
├── README.md
├── .gitignore
└── backend/
    ├── android/                 # Capacitor Android project
    ├── assets/                  # Source app icon for Capacitor asset generation
    ├── capacitor.config.json
    ├── database/
    │   └── schema.sql
    ├── frontend/
    │   ├── index.html
    │   ├── config.js            # Configurable API base URL
    │   ├── script.js
    │   └── styles.css
    ├── src/
    │   ├── main/java/com/studentmanagement/backend/
    │   │   ├── BackendApplication.java
    │   │   ├── controller/
    │   │   ├── exception/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── main/resources/application.properties
    │   └── test/
    ├── mvnw
    ├── mvnw.cmd
    ├── package.json             # Capacitor and Playwright tooling
    └── pom.xml
```

## Requirements

- JDK 21 (LTS) and the included Maven Wrapper.
- MySQL 8 or later.
- Git to clone the repository.

Before running Maven, confirm `java -version` reports Java 21. On Windows, set `JAVA_HOME` to the installed JDK 21 directory and add its `bin` directory to `PATH` for the current terminal.

## Database setup

Start MySQL, then create the database and student table using the script. From Command Prompt or macOS/Linux:

```bash
mysql -u root -p < backend/database/schema.sql
```

From PowerShell:

```powershell
cmd /c "mysql -u root -p < backend\database\schema.sql"
```

The script creates `student_management` and a `students` table with a unique email address. Hibernate updates the schema on application startup; student records are stored in MySQL and remain after restarts. For an application account instead of using the MySQL administrator, create a dedicated user with privileges limited to this database:

```sql
CREATE USER 'student_app'@'localhost' IDENTIFIED BY 'replace-with-a-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
  ON student_management.* TO 'student_app'@'localhost';
```

Set the required `DB_USERNAME` and `DB_PASSWORD` environment variables before starting the backend. Spring fails early with an unresolved-property message if either required value is missing; it does not fall back to a blank username or password. Optional variables are `DB_HOST` (default `localhost`), `DB_PORT` (default `3306`), `DB_NAME` (default `student_management`), and `SERVER_PORT` (default `8080`). Do not commit credentials.

There are no default admin or student passwords. The admin account can be created on first startup with `ADMIN_EMAIL` and `ADMIN_PASSWORD`; these values do not change the password of an existing account. Student accounts are created by registering against an existing roster ID, email, and date of birth. Login uses the stored BCrypt password hash. No email-based password recovery service is configured.

PowerShell example:

```powershell
$env:DB_USERNAME = Read-Host "MySQL username"
$secureDbPassword = Read-Host "MySQL password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secureDbPassword).Password
```

### If you forgot the MySQL root password

The application does not need to use the MySQL root account. If another authorized database administrator account exists, use that account to create the restricted `student_app` user above. Otherwise, recover the root account using the procedure for your installed MySQL version:

1. Identify the installed MySQL Server version and data directory. On Windows, inspect the `MySQL80` service's executable path and its `--defaults-file`; the service may have a different name on your computer.
2. Use the MySQL Installer's server reconfiguration/password recovery option if it is available for that installation. Otherwise, follow MySQL's official [Windows root-password reset procedure for MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/resetting-permissions.html); use the matching documentation version if the installed server differs.
3. Back up the MySQL data directory before maintenance, stop the MySQL service as directed by the procedure, and reset only the `root` account password. Do **not** uninstall MySQL, initialize a new data directory, delete files under the data directory, or run `DROP DATABASE`.
4. Restart the existing MySQL service and verify access by entering the new password at the `mysql -u root -p` prompt. Then create the restricted application user and use that user's credentials in `DB_USERNAME` and `DB_PASSWORD`.
5. Remove any temporary reset files created by the official procedure immediately afterward; keep them access-restricted because they may contain the new password. Never commit or share those files.

Resetting an account password does not require deleting the `student_management` database or its student records. If the service configuration or data directory is uncertain, stop and consult the database administrator before changing MySQL startup options.

## Run locally

From the repository root, initialize the database, set the database environment variables above, and configure a one-time admin bootstrap account using `ADMIN_EMAIL` and `ADMIN_PASSWORD`. Both values must be supplied together; passwords must be 12–72 UTF-8 bytes. There are no built-in credentials. If neither value is provided, the application starts without an admin account. The bootstrap does not overwrite the password of an existing account.

For local HTTP development only, set `SESSION_COOKIE_SECURE=false`; HTTPS deployments should keep the secure-cookie default. The session cookie is HttpOnly, SameSite=Lax by default, and expires after 30 minutes of inactivity. Passwords are stored with BCrypt (work factor 12). Student self-registration requires a matching existing roster student ID, email, and date of birth; the password is never used as a substitute for roster identity.

If a separately hosted frontend or Capacitor WebView calls a cross-site API, configure `SESSION_COOKIE_SAME_SITE=None` only with HTTPS (`SESSION_COOKIE_SECURE=true`), and set `APP_CORS_ALLOWED_ORIGINS` to the exact trusted origin(s). The CSRF header remains required.

Example PowerShell setup (enter a unique, local development password interactively):

```powershell
$env:ADMIN_EMAIL = Read-Host "Local admin email"
$secureAdminPassword = Read-Host "Local admin password" -AsSecureString
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $secureAdminPassword).Password
$env:SESSION_COOKIE_SECURE = "false"
```

The browser stores only the HttpOnly session cookie; CSRF tokens are held in memory and sent in `X-CSRF-TOKEN` for state-changing requests. Admins can manage the full roster. Student accounts can read their own roster profile and update only their phone number and address. When an admin changes a roster email, the linked student login email is updated as well.

### Recover an existing account password

An operator with access to the application host and database can reset an existing admin or student account without exposing a reset endpoint. Stop the running application first. From the `backend` directory, set the database credentials as usual, then enter the target account email and new password in the PowerShell prompts:

```powershell
$env:DB_USERNAME = Read-Host "MySQL username"
$secureDbPassword = Read-Host "MySQL password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secureDbPassword).Password
$env:ACCOUNT_RESET_EMAIL = Read-Host "Existing account email"
$secureResetPassword = Read-Host "New password (12-72 UTF-8 bytes)" -AsSecureString
$env:ACCOUNT_RESET_PASSWORD = [System.Net.NetworkCredential]::new('', $secureResetPassword).Password
$env:ACCOUNT_RESET_ENABLED = "true"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"
```

The one-shot non-web run updates only the matching account's BCrypt hash; it does not create an account or modify/delete student records. Confirm the reset-completed message, then immediately clear the reset values:

```powershell
Remove-Item Env:ACCOUNT_RESET_ENABLED, Env:ACCOUNT_RESET_EMAIL, Env:ACCOUNT_RESET_PASSWORD
```

If the target account is not found or the new password is invalid, the reset fails rather than creating an account. Restart the normal application after the reset and sign in with the new password. This operation invalidates sessions only when the running application is stopped; stopping it before resetting ensures prior in-memory sessions are gone.

The initial student-claim baseline compares roster ID, email, and date of birth; no email-delivery or invitation-code service is configured here. Those roster attributes are not proof of mailbox ownership. Before exposing self-registration to untrusted networks, add a verified email/one-time invitation flow; otherwise restrict registration to a trusted enrollment context.

Run the backend from a PowerShell terminal after MySQL is configured and the application database user exists:

```powershell
Set-Location C:\Users\Hp\Documents\StudentManagementSystem\backend
$env:JAVA_HOME = 'C:\Users\Hp\.jdk\temurin-21-capacitor'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version

$env:DB_USERNAME = Read-Host "MySQL application username"
$secureDbPassword = Read-Host "MySQL application password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secureDbPassword).Password
$env:SESSION_COOKIE_SECURE = "false"

# Only if an admin account does not already exist:
$env:ADMIN_EMAIL = Read-Host "Initial admin email"
$secureAdminPassword = Read-Host "Initial admin password (12-72 UTF-8 bytes)" -AsSecureString
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $secureAdminPassword).Password

.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The secure prompts do not display passwords. These environment values apply only to this PowerShell window; keep it open while the server runs. If an admin already exists, omit the two `ADMIN_*` assignments (the bootstrap does not reset existing passwords). Do **not** type `spring.datasource.username=${DB_USERNAME}` into PowerShell: it is a Spring property, not a shell command, and is already configured in `src/main/resources/application.properties`.

In VS Code, the workspace also provides **Maven: clean verify (wrapper)**, **Maven: compile (wrapper)**, and **Spring Boot: run (wrapper)** tasks. The run task prompts for a MySQL application username and a masked password when they are not already set, then starts the app with `mvnw.cmd`.

After startup, verify the site from a second PowerShell window:

```powershell
Invoke-WebRequest http://localhost:8080/ -UseBasicParsing
Get-NetTCPConnection -State Listen -LocalPort 8080
```

Only open [http://localhost:8080](http://localhost:8080) once the request succeeds and port 8080 is listening. If startup reports an access-denied or unknown-database error, stop and resolve the MySQL account/database setup; do not switch to H2 or use invented credentials.

On macOS/Linux, use `./mvnw` in place of `.\mvnw.cmd`. Open [http://localhost:8080](http://localhost:8080); the backend serves the frontend and API on the same origin. Do not open `index.html` directly from the filesystem because API requests require the running application.

To package and run the application:

```powershell
.\mvnw.cmd clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Keep the `frontend/` directory alongside the application working directory when using the packaged JAR.

## Testing

Run the deterministic JavaScript auth-client unit tests and backend security/API tests with Java 21:

```powershell
cd backend
npm test
.\mvnw.cmd test
```

For browser end-to-end testing, start the backend as described above and set `E2E_ADMIN_USERNAME` and `E2E_ADMIN_PASSWORD` to the environment-only admin account for the local test instance. The roster CRUD E2E tests are skipped when these variables are not set. In a second terminal:

```powershell
cd backend
npm ci
npx playwright install chromium
npm run test:e2e
```

The browser suite uses uniquely named temporary records and removes them after each test. It exercises CRUD, search, pagination, reload persistence, navigation, form controls, and a mobile viewport. Do not run these tests against production data.

## Android app (Capacitor)

The Android app reuses `backend/frontend/`; the Spring Boot Maven backend and MySQL database remain the server and persistent store. Capacitor 8 packages the existing frontend into the Android WebView. The Android application ID is `com.dharshana.studentmanagement` and the app label is `Student Management System`.

### Configure the backend URL

The website keeps same-origin API requests by default. Before building the Android app, edit `backend/frontend/config.js` and set `apiBaseUrl` to the **HTTPS origin** of a backend reachable from the device:

```js
window.APP_CONFIG = Object.freeze({
  apiBaseUrl: 'https://api.your-domain.example',
});
```

The frontend appends API paths to the configured origin. Replace the example with your real deployed API origin; do not use `localhost` for a production APK. A phone's `localhost` points to the phone itself, not the computer hosting the backend. The backend must be reachable by the device and use HTTPS. For the cross-site Capacitor session, configure `SESSION_COOKIE_SAME_SITE=None` and keep `SESSION_COOKIE_SECURE=true`. Configure `APP_CORS_ALLOWED_ORIGINS` on the backend to include the Capacitor Android WebView origin (`https://localhost`) and any website origins that call the API, comma-separated. Keep database credentials only in the backend environment (`DB_USERNAME` and `DB_PASSWORD`); they are never placed in the APK.

### Requirements

- Node.js 22 or later and npm.
- Android Studio or Android SDK command-line tools, Android SDK Platform 36, and Android Build Tools.
- JDK 21 for both the Spring backend and generated Android Gradle project.
- An Android emulator or device for install/runtime testing.

### Generate assets and build a debug APK

From `backend/` in PowerShell, after installing Android Studio/SDK and configuring `ANDROID_HOME` or `ANDROID_SDK_ROOT`:

```powershell
npm ci
npm run assets:android
npx cap sync android
Set-Location android
.\gradlew.bat assembleDebug
```

The APK is written to `backend/android/app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or open the Android project with `npm run cap:open`. After changing frontend assets or `frontend/config.js`, rerun `npx cap sync android` and rebuild. The launcher icon and splash-screen resources are generated from `backend/assets/logo.svg`.

## REST API

Base path: `/api/students`

| Method | Path | Description | Success |
|---|---|---|---|
| `GET` | `/api/students` | List students | `200 OK` |
| `GET` | `/api/students/{id}` | Get one student's details | `200 OK` |
| `POST` | `/api/students` | Create a student | `201 Created` |
| `PUT` | `/api/students/{id}` | Update a student | `200 OK` |
| `DELETE` | `/api/students/{id}` | Delete a student | `204 No Content` |
| `GET` | `/api/students/me` | Read the signed-in student's own roster profile | `200 OK` |
| `PUT` | `/api/students/me` | Update the signed-in student's phone and address only | `200 OK` |

Authentication endpoints: `GET /api/auth/csrf`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/session`, and `POST /api/auth/logout`. Every state-changing request requires a valid CSRF token. Student account registration is limited to existing roster records.

Student fields are `name`, `email`, `phone`, `department`, `course`, and `year`; optional fields are `dateOfBirth` (`YYYY-MM-DD`), `address`, and `admissionDate` (`YYYY-MM-DD`). Invalid requests return `400` with field errors, duplicate email addresses return `409`, and unknown IDs return `404`.

## Deployment

Provision a Java 21 runtime and a MySQL database with network access from the application host. Apply `backend/database/schema.sql`, configure the database and `SERVER_PORT` environment variables, and provide admin bootstrap credentials through the environment only. Build with `.\mvnw.cmd clean package`, and run the generated JAR from the `backend` directory. Serve the application behind an HTTPS-enabled reverse proxy, retain secure session cookies, and restrict database access to the application host. No cloud environment or hosting credentials are configured in this repository, so a live deployment URL is not available yet.

## Screenshots

Add screenshots of the dashboard, student list, registration form, and student details here after running the application.
