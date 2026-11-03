package com.studentmanagement.backend;

import com.studentmanagement.backend.repository.StudentRepository;
import com.studentmanagement.backend.model.Student;
import com.studentmanagement.backend.model.UserAccount;
import com.studentmanagement.backend.model.AccountRole;
import com.studentmanagement.backend.repository.UserAccountRepository;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:student_management_test;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class BackendApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearStudents() {
		userAccountRepository.deleteAll();
		studentRepository.deleteAll();
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void supportsStudentCrud() throws Exception {
		String email = "student-" + UUID.randomUUID() + "@example.com";
		MvcResult createResult = mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf())
						.content(studentJson("Morgan Student", email, 2)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Morgan Student"))
				.andExpect(jsonPath("$.dateOfBirth").value("2004-01-15"))
				.andExpect(jsonPath("$.admissionDate").value("2025-09-01"))
				.andReturn();
		JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
		int id = created.get("id").asInt();

		mockMvc.perform(get("/api/students"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value(email));
		mockMvc.perform(get("/api/students/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));
		mockMvc.perform(put("/api/students/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf())
						.content(studentJson("Morgan Updated", email, 3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Morgan Updated"))
				.andExpect(jsonPath("$.year").value(3));
		mockMvc.perform(delete("/api/students/{id}", id).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/students/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsInvalidStudentAndDuplicateEmail() throws Exception {
		mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf())
						.content("{\"name\":\"\",\"email\":\"not-an-email\",\"phone\":\"bad\",\"department\":\"\",\"course\":\"\",\"year\":11}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.year").exists());

		String email = "duplicate-" + UUID.randomUUID() + "@example.com";
		mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf())
						.content(studentJson("Morgan Student", email, 1)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf())
						.content(studentJson("Another Student", email.toUpperCase(), 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void keepsStudentAccountEmailInSyncWhenAdminChangesRosterEmail() throws Exception {
		String originalEmail = "student-" + UUID.randomUUID() + "@example.test";
		String updatedEmail = "updated-" + UUID.randomUUID() + "@example.test";
		Student student = new Student();
		student.setName("Morgan Student");
		student.setEmail(originalEmail);
		student.setPhone("+1 555 123 4567");
		student.setDepartment("Engineering");
		student.setCourse("Computer Science");
		student.setYear(2);
		student = studentRepository.saveAndFlush(student);
		userAccountRepository.saveAndFlush(new UserAccount(
				originalEmail,
				passwordEncoder.encode("correct-horse-battery"),
				AccountRole.STUDENT,
				student));

		mockMvc.perform(put("/api/students/{id}", student.getId())
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
								.user("admin").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(studentJson("Morgan Student", updatedEmail, 3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(updatedEmail));

		UserAccount updatedAccount = userAccountRepository.findByStudent_Id(student.getId()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(updatedEmail, updatedAccount.getEmail());
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new LoginRequest(updatedEmail, "correct-horse-battery"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("STUDENT"));
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new LoginRequest(originalEmail, "correct-horse-battery"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void allowsCapacitorWebViewApiRequests() throws Exception {
		mockMvc.perform(options("/api/students")
						.header("Origin", "https://localhost")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("Access-Control-Allow-Origin", "https://localhost"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
		mockMvc.perform(options("/api/auth/login")
						.header("Origin", "https://localhost")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("Access-Control-Allow-Credentials", "true"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("x-csrf-token")));
	}

	@Test
	void requiresAuthenticationAndCsrfForRosterAdministration() throws Exception {
		mockMvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.content(studentJson("Unauthorized", "unauth@example.test", 1)))
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectsStateChangingRequestsWithoutCsrfEvenForAdmins() throws Exception {
		mockMvc.perform(post("/api/students")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(studentJson("No CSRF", "csrf@example.test", 1)))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentRegistrationIsRosterBoundAndStudentRoleCanOnlyReadOwnRecord() throws Exception {
		Student rosterEntry = new Student();
		rosterEntry.setName("Roster Student");
		rosterEntry.setEmail("roster@example.test");
		rosterEntry.setPhone("+1 555 123 4567");
		rosterEntry.setDepartment("Engineering");
		rosterEntry.setCourse("Computer Science");
		rosterEntry.setYear(2);
		rosterEntry.setDateOfBirth(java.time.LocalDate.of(2004, 1, 15));
		rosterEntry = studentRepository.saveAndFlush(rosterEntry);

		String registration = objectMapper.writeValueAsString(new RegistrationRequest(
				rosterEntry.getId(), rosterEntry.getEmail(), "2004-01-15", "correct-horse-battery"));
		MvcResult csrfInitialization = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		String csrfToken = objectMapper.readTree(csrfInitialization.getResponse().getContentAsString())
				.get("token").asText();
		MockHttpSession csrfSession = (MockHttpSession) csrfInitialization.getRequest().getSession(false);
		org.junit.jupiter.api.Assertions.assertNotNull(csrfSession);
		mockMvc.perform(post("/api/auth/register")
						.session(csrfSession)
						.header("X-CSRF-TOKEN", csrfToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(registration))
				.andExpect(status().isCreated());

		UserAccount account = userAccountRepository.findByEmailIgnoreCase(rosterEntry.getEmail()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(AccountRole.STUDENT, account.getRole());
		org.junit.jupiter.api.Assertions.assertNotEquals("correct-horse-battery", account.getPasswordHash());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("correct-horse-battery", account.getPasswordHash()));

		mockMvc.perform(post("/api/auth/register")
						.session(csrfSession)
						.header("X-CSRF-TOKEN", csrfToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegistrationRequest(
								rosterEntry.getId(), rosterEntry.getEmail(), "2000-01-01", "correct-horse-battery"))))
				.andExpect(status().isBadRequest());

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.session(csrfSession)
						.header("X-CSRF-TOKEN", csrfToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(rosterEntry.getEmail(), "correct-horse-battery"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("STUDENT"))
				.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		mockMvc.perform(get("/api/students/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(rosterEntry.getId()));
		MvcResult profileCsrfResult = mockMvc.perform(get("/api/auth/csrf").session(session))
				.andExpect(status().isOk())
				.andReturn();
		String profileCsrf = objectMapper.readTree(profileCsrfResult.getResponse().getContentAsString())
				.get("token").asText();
		mockMvc.perform(put("/api/students/me")
						.session(session)
						.header("X-CSRF-TOKEN", profileCsrf)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"phone\":\"+1 555 999 0000\",\"address\":\"Updated address\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone").value("+1 555 999 0000"))
				.andExpect(jsonPath("$.address").value("Updated address"))
				.andExpect(jsonPath("$.department").value("Engineering"));
		mockMvc.perform(put("/api/students/{id}", rosterEntry.getId())
						.session(session)
						.header("X-CSRF-TOKEN", profileCsrf)
						.contentType(MediaType.APPLICATION_JSON)
						.content(studentJson("Escalation Attempt", rosterEntry.getEmail(), 10)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/students").session(session))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/auth/logout")
						.session(session)
						.header("X-CSRF-TOKEN", profileCsrf))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/auth/session").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void resetsAnExistingAdminPasswordOnlyWhenExplicitlyEnabled() throws Exception {
		String email = "admin-" + UUID.randomUUID() + "@example.test";
		String oldPassword = "old-admin-password";
		String newPassword = "new-admin-password";
		userAccountRepository.saveAndFlush(new UserAccount(
				email, passwordEncoder.encode(oldPassword), AccountRole.ADMIN, null));

		new com.studentmanagement.backend.config.AccountPasswordResetBootstrap(
				false, "", "", userAccountRepository, passwordEncoder)
				.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));
		UserAccount unchanged = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(oldPassword, unchanged.getPasswordHash()));

		new com.studentmanagement.backend.config.AccountPasswordResetBootstrap(
				true, email, newPassword, userAccountRepository, passwordEncoder)
				.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));
		UserAccount reset = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
		org.junit.jupiter.api.Assertions.assertFalse(passwordEncoder.matches(oldPassword, reset.getPasswordHash()));
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(newPassword, reset.getPasswordHash()));

		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(email, newPassword))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(email, oldPassword))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void resetsStudentPasswordWithoutDeletingItsRosterRecord() throws Exception {
		Student student = new Student();
		student.setName("Roster Student");
		student.setEmail("student-" + UUID.randomUUID() + "@example.test");
		student.setPhone("+1 555 123 4567");
		student.setDepartment("Engineering");
		student.setCourse("Computer Science");
		student.setYear(2);
		student.setDateOfBirth(java.time.LocalDate.of(2004, 1, 15));
		student = studentRepository.saveAndFlush(student);
		String oldPassword = "old-student-password";
		String newPassword = "new-student-password";
		userAccountRepository.saveAndFlush(new UserAccount(
				student.getEmail(), passwordEncoder.encode(oldPassword), AccountRole.STUDENT, student));

		new com.studentmanagement.backend.config.AccountPasswordResetBootstrap(
				true, student.getEmail(), newPassword, userAccountRepository, passwordEncoder)
				.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(student.getEmail(), newPassword))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("STUDENT"))
				.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		mockMvc.perform(get("/api/students/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(student.getId()));
		org.junit.jupiter.api.Assertions.assertTrue(studentRepository.existsById(student.getId()));
		org.junit.jupiter.api.Assertions.assertFalse(passwordEncoder.matches(
				oldPassword, userAccountRepository.findByEmailIgnoreCase(student.getEmail()).orElseThrow().getPasswordHash()));
	}

	@Test
	void resetModeDoesNotCreateAnAccountWhenTheTargetIsMissing() {
		String email = "missing-" + UUID.randomUUID() + "@example.test";
		org.springframework.boot.DefaultApplicationArguments arguments =
				new org.springframework.boot.DefaultApplicationArguments(new String[0]);

		org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
				() -> new com.studentmanagement.backend.config.AccountPasswordResetBootstrap(
						true, email, "new-account-password", userAccountRepository, passwordEncoder).run(arguments));
		new com.studentmanagement.backend.config.AdminAccountBootstrap(
				email, "bootstrap-admin-password", true, userAccountRepository, passwordEncoder).run(arguments);

		org.junit.jupiter.api.Assertions.assertTrue(userAccountRepository.findByEmailIgnoreCase(email).isEmpty());
	}

	private String studentJson(String name, String email, int year) throws Exception {
		return objectMapper.writeValueAsString(new StudentRequest(
				name,
				email,
				"+1 555 123 4567",
				"Engineering",
				"Computer Science",
				year,
				"2004-01-15",
				"100 Campus Way",
				"2025-09-01"));
	}

	private record RegistrationRequest(Integer studentId, String email, String dateOfBirth, String password) {
	}

	private record LoginRequest(String email, String password) {
	}

	private record StudentRequest(
			String name,
			String email,
			String phone,
			String department,
			String course,
			int year,
			String dateOfBirth,
			String address,
			String admissionDate) {
	}
}
