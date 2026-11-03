package com.studentmanagement.backend;

import com.studentmanagement.backend.repository.StudentRepository;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

	@BeforeEach
	void clearStudents() {
		studentRepository.deleteAll();
	}

	@Test
	void supportsStudentCrud() throws Exception {
		String email = "student-" + UUID.randomUUID() + "@example.com";
		MvcResult createResult = mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
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
						.content(studentJson("Morgan Updated", email, 3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Morgan Updated"))
				.andExpect(jsonPath("$.year").value(3));
		mockMvc.perform(delete("/api/students/{id}", id))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/students/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsInvalidStudentAndDuplicateEmail() throws Exception {
		mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\",\"email\":\"not-an-email\",\"phone\":\"bad\",\"department\":\"\",\"course\":\"\",\"year\":11}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.year").exists());

		String email = "duplicate-" + UUID.randomUUID() + "@example.com";
		mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.content(studentJson("Morgan Student", email, 1)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/students")
						.contentType(MediaType.APPLICATION_JSON)
						.content(studentJson("Another Student", email.toUpperCase(), 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").exists());
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
