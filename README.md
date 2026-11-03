# Student Management System

A full-stack student records application with a responsive dashboard, a Spring Boot REST API, and MySQL persistence. The backend serves the existing HTML, CSS, and JavaScript frontend and validates every student create/update request.

## Features

- Dashboard statistics, searchable and filterable student list, and pagination.
- Register, view details, edit, and delete student records with confirmation.
- Client- and server-side validation, clear API errors, loading indicators, and notifications.
- MySQL persistence with a versioned schema script.
- Responsive interface for desktop and mobile.

## Technologies

- Java 25 (LTS)
- Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Jakarta Validation
- MySQL 8+
- Maven Wrapper
- HTML5, CSS3, and vanilla JavaScript

## Project structure

```text
StudentManagementSystem/
├── README.md
├── .gitignore
└── backend/
    ├── database/
    │   └── schema.sql
    ├── frontend/
    │   ├── index.html
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
    └── pom.xml
```

## Requirements

- JDK 25 (LTS) and the included Maven Wrapper.
- MySQL 8 or later.
- Git to clone the repository.

Before running Maven, confirm `java -version` reports Java 25. On Windows, set `JAVA_HOME` to the installed JDK 25 directory and add its `bin` directory to `PATH` for the current terminal.

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

Set the required `DB_USERNAME` and `DB_PASSWORD` environment variables before starting the backend. Optional variables are `DB_HOST` (default `localhost`), `DB_PORT` (default `3306`), `DB_NAME` (default `student_management`), and `SERVER_PORT` (default `8080`). Do not commit credentials.

PowerShell example:

```powershell
$env:DB_USERNAME = "student_app"
$env:DB_PASSWORD = "your-local-mysql-password"
```

## Run locally

From the repository root, initialize the database, set the database environment variables above, and run:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

On macOS/Linux, use `./mvnw` in place of `.\mvnw.cmd`. Open [http://localhost:8080](http://localhost:8080); the backend serves the frontend and API on the same origin. Do not open `index.html` directly from the filesystem because API requests require the running application.

To package and run the application:

```powershell
.\mvnw.cmd clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Keep the `frontend/` directory alongside the application working directory when using the packaged JAR.

## REST API

Base path: `/api/students`

| Method | Path | Description | Success |
|---|---|---|---|
| `GET` | `/api/students` | List students | `200 OK` |
| `GET` | `/api/students/{id}` | Get one student's details | `200 OK` |
| `POST` | `/api/students` | Create a student | `201 Created` |
| `PUT` | `/api/students/{id}` | Update a student | `200 OK` |
| `DELETE` | `/api/students/{id}` | Delete a student | `204 No Content` |

Student fields are `name`, `email`, `phone`, `department`, `course`, and `year`; optional fields are `dateOfBirth` (`YYYY-MM-DD`), `address`, and `admissionDate` (`YYYY-MM-DD`). Invalid requests return `400` with field errors, duplicate email addresses return `409`, and unknown IDs return `404`.

## Deployment

Provision a Java 25 runtime and a MySQL database with network access from the application host. Apply `backend/database/schema.sql`, configure the database and `SERVER_PORT` environment variables, build with `.\mvnw.cmd clean package`, and run the generated JAR from the `backend` directory. Serve the application behind an HTTPS-enabled reverse proxy and restrict database access to the application host. The API does not include authentication; keep it on a trusted network or put it behind an authenticated gateway before internet exposure. No cloud environment or hosting credentials are configured in this repository, so a live deployment URL is not available yet.

## Screenshots

Add screenshots of the dashboard, student list, registration form, and student details here after running the application.
