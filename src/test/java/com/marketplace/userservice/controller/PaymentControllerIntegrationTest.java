package com.marketplace.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.userservice.RedisTestConfig;
import com.marketplace.userservice.dto.CreateCardRequest;
import com.marketplace.userservice.dto.CreateUserRequest;
import com.marketplace.userservice.dto.UpdateCardRequest;
import com.marketplace.userservice.dto.UserResponseDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // <-- ДОБАВИТЬ
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(RedisTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long testUserId;
    private static Long createdCardId;

    @BeforeAll
    void setUp() throws Exception {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .name("Тест")
                .surname("Тестов")
                .email("test@test.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponseDto user = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponseDto.class);
        testUserId = user.getId();
    }

    @Test
    @Order(1)
    void shouldCreateCardSuccessfully() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("1234567890123456")
                .holder("TEST USER")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/" + testUserId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("1234567890123456"))
                .andExpect(jsonPath("$.holder").value("TEST USER"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        createdCardId = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.marketplace.userservice.dto.CardResponseDto.class).getId();
        assertNotNull(createdCardId);
    }

    @Test
    @Order(2)
    void shouldGetCardById() throws Exception {
        mockMvc.perform(get("/api/cards/" + createdCardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCardId))
                .andExpect(jsonPath("$.number").value("1234567890123456"));
    }

    @Test
    @Order(3)
    void shouldGetCardsByUserId() throws Exception {
        mockMvc.perform(get("/api/users/" + testUserId + "/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Order(4)
    void shouldUpdateCardSuccessfully() throws Exception {
        UpdateCardRequest request = UpdateCardRequest.builder()
                .holder("UPDATED HOLDER")
                .build();

        mockMvc.perform(put("/api/cards/" + createdCardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("UPDATED HOLDER"));
    }

    @Test
    @Order(5)
    void shouldEnforceCardLimit() throws Exception {
        // Создаем еще 4 карты (итого 5)
        for (int i = 0; i < 4; i++) {
            CreateCardRequest request = CreateCardRequest.builder()
                    .number("123456789012345" + (i + 2))
                    .holder("TEST USER " + i)
                    .expirationDate(LocalDate.now().plusYears(2))
                    .build();

            mockMvc.perform(post("/api/users/" + testUserId + "/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        CreateCardRequest request = CreateCardRequest.builder()
                .number("1234567890123459")
                .holder("TEST USER 5")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        mockMvc.perform(post("/api/users/" + testUserId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User cannot have more than 5 active cards"));
    }

    @Test
    @Order(6)
    void shouldDeleteCardSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/cards/" + createdCardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/" + createdCardId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void shouldReturn404ForNonExistentCard() throws Exception {
        mockMvc.perform(get("/api/cards/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void shouldValidateCreateCardRequest() throws Exception {
        CreateCardRequest invalidRequest = CreateCardRequest.builder()
                .number("")
                .holder("")
                .expirationDate(LocalDate.now().minusYears(1))
                .build();

        mockMvc.perform(post("/api/users/" + testUserId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}