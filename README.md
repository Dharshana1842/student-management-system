# Student Management System

## Description
This project is a full-stack Student Management System built with Spring Boot for the backend and plain HTML, CSS, and JavaScript for the frontend. It allows users to add, view, edit, filter, search, paginate, and delete student records through a responsive dashboard interface.

## Features
- Dashboard overview with summary cards
- Add, view, edit, and delete students
- Search by name, email, phone, department, or course
- Filter by department, course, and year
- Pagination and page size control
- Form validation for required fields
- Loading indicators and toast notifications
- Responsive design for desktop and mobile

## Technologies Used
- Java 17
- Spring Boot 4.1.1
- Spring Data JPA
- MySQL
- Maven
- HTML5
- CSS3
- JavaScript (Vanilla JS)

## Project Structure
```text
StudentManagementSystem/
├── backend/
│   ├── src/
│   ├── frontend/
│   ├── pom.xml
│   ├── .gitignore
│   └── mvnw
├── .gitignore
├── README.md
└── .git/
```

## Backend Setup
1. Create a MySQL database named `student_management`.
2. Update environment variables for database credentials if needed:
   - `DB_USERNAME`
   - `DB_PASSWORD`
3. Run the backend from the `backend` folder:

```bash
cd backend
./mvnw spring-boot:run
```

## Frontend Setup
The frontend files are served locally as static files from the `backend/frontend` folder. Open `frontend/index.html` in a browser or use a simple local web server if needed.

## Database Setup
Make sure MySQL is installed and running. The backend config expects a database named `student_management`.

Example:
```sql
CREATE DATABASE student_management;
```

## API Endpoints
- `GET /api/students`
- `GET /api/students/{id}`
- `POST /api/students`
- `PUT /api/students/{id}`
- `DELETE /api/students/{id}`

## How to Run
1. Start MySQL.
2. Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

3. Open the frontend HTML page in a browser:

```text
backend/frontend/index.html
```

4. The frontend will communicate with:

```text
http://localhost:8080/api/students
```

## Screenshots
Placeholder for screenshots of the dashboard, form, student list, and filters.

## Future Enhancements
- Authentication and authorization
- Export to CSV/PDF
- Advanced analytics charts
- Role-based access control
- Cloud deployment support
