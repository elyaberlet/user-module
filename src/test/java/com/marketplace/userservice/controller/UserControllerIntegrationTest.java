
package com.marketplace.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.userservice.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createUserJson(String name, String email) throws Exception {
        return objectMapper.writeValueAsString(
                new CreateUserRequest(
                        name,
                        "Ivanov",
                        LocalDate.of(1995, 5, 10),
                        email
                )
        );
    }

    private String createUserJson(String email) throws Exception {
        return createUserJson("Ivan", email);
    }

    private long createUser(String email) throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(email)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void createUser_shouldReturn201AndLocation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson("ivan@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.surname").value("Ivanov"))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        long userId = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asLong();

        String location = result.getResponse().getHeader("Location");

        assertNotNull(location, "Заголовок Location должен присутствовать");
        assertTrue(
                location.endsWith("/api/users/" + userId),
                "Location должен содержать ID созданного пользователя"
        );
    }

    @Test
    void createUser_shouldReturn400_whenRequestIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "surname": "Ivanov",
                  "birthDate": "1995-05-10",
                  "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUser_shouldReturn200_whenUserExists() throws Exception {
        long userId = createUser("get-user@example.com");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.email").value("get-user@example.com"));
    }

    @Test
    void getUsers_shouldReturnPage() throws Exception {
        createUser("first@example.com");
        createUser("second@example.com");

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getUsers_shouldFilterByName() throws Exception {
        createUserJsonAndSave("Ivan", "filter-ivan@example.com");
        createUserJsonAndSave("Petr", "filter-petr@example.com");

        mockMvc.perform(get("/api/users")
                        .param("name", "Ivan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Ivan"))
                .andExpect(jsonPath("$.content[0].email")
                        .value("filter-ivan@example.com"));
    }

    private void createUserJsonAndSave(String name, String email)
            throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(name, email)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        long userId = createUser("update-user@example.com");

        String updateJson = """
                {
                  "name": "Petr",
                  "surname": "Petrov",
                  "birthDate": "1994-03-15",
                  "email": "updated-user@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Petr"))
                .andExpect(jsonPath("$.surname").value("Petrov"))
                .andExpect(jsonPath("$.email").value("updated-user@example.com"));
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        long userId = createUser("delete-user@example.com");

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void updateStatus_shouldReturn204() throws Exception {
        long userId = createUser("status-user@example.com");

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .param("active", "false"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }
}
