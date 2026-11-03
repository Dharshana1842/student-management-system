CREATE DATABASE IF NOT EXISTS student_management
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE student_management;

CREATE TABLE IF NOT EXISTS students (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(254) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  department VARCHAR(100) NOT NULL,
  course VARCHAR(120) NOT NULL,
  year INT NOT NULL,
  date_of_birth DATE,
  address VARCHAR(500),
  admission_date DATE,
  PRIMARY KEY (id),
  CONSTRAINT uk_students_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(16) NOT NULL,
  student_id INT,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_accounts_email UNIQUE (email),
  CONSTRAINT uk_user_accounts_student UNIQUE (student_id),
  CONSTRAINT fk_user_accounts_student FOREIGN KEY (student_id) REFERENCES students(id)
);
