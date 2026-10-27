
package com.marketplace.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.userservice.dto.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = webAppContextSetup(context)
                .defaultRequest(get("/")
                        .with(jwt()
                                .jwt(token -> token
                                        .claim("role", "ADMIN")
                                        .claim("user_id", 1L))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                )))
                .apply(springSecurity())
                .build();
    }

    private long createUser() throws Exception {
        String userJson = objectMapper.writeValueAsString(
                new CreateUserRequest(
                        "Ivan",
                        "Ivanov",
                        LocalDate.of(1995, 5, 10),
                        "card-owner@example.com"
                )
        );

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createCard(long userId, String number) throws Exception {
        String cardJson = """
                {
                  "number": "%s",
                  "holder": "IVAN IVANOV",
                  "expirationDate": "%s"
                }
                """.formatted(number, LocalDate.now().plusYears(2));

        String response = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void createCard_shouldReturn201AndLocation() throws Exception {
        long userId = createUser();

        String cardJson = """
                {
                  "number": "1234567812345678",
                  "holder": "IVAN IVANOV",
                  "expirationDate": "%s"
                }
                """.formatted(LocalDate.now().plusYears(2));

        String response = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.number").value("1234567812345678"))
                .andExpect(jsonPath("$.holder").value("IVAN IVANOV"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long cardId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/cards/{cardId}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));
    }

    @Test
    void createCard_shouldReturn400_whenRequestIsInvalid() throws Exception {
        long userId = createUser();

        String invalidJson = """
                {
                  "number": "123",
                  "holder": "",
                  "expirationDate": "2020-01-01"
                }
                """;

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCard_shouldReturn200_whenCardExists() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId, "1111222233334444");

        mockMvc.perform(get("/api/cards/{cardId}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.number").value("1111222233334444"));
    }

    @Test
    void getCards_shouldReturnPage() throws Exception {
        long userId = createUser();
        createCard(userId, "1111222233334444");
        createCard(userId, "2222333344445555");

        mockMvc.perform(get("/api/users/{userId}/cards", userId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void updateCard_shouldReturnUpdatedCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId, "1111222233334444");

        String updateJson = """
                {
                  "number": "9999888877776666",
                  "holder": "NEW HOLDER",
                  "expirationDate": "%s"
                }
                """.formatted(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.number").value("9999888877776666"))
                .andExpect(jsonPath("$.holder").value("NEW HOLDER"));
    }

    @Test
    void deleteCard_shouldReturn204() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId, "1111222233334444");

        mockMvc.perform(delete("/api/cards/{cardId}", cardId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/cards/{cardId}", cardId))
                .andExpect(status().isNotFound());
    }
}
