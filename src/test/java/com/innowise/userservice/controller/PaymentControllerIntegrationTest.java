package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.AbstractIntegrationTest;
import com.innowise.userservice.dto.CreateUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    private final List<Long> createdUserIds = new ArrayList<>();

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
                        "card-owner-" + System.nanoTime() + "@example.com"
                )
        );

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long userId = objectMapper.readTree(response).get("id").asLong();
        createdUserIds.add(userId);

        return userId;
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

    @AfterEach
    void cleanUp() {
        for (Long userId : createdUserIds) {
            jdbcTemplate.update(
                    "DELETE FROM payment_cards WHERE user_id = ?",
                    userId
            );

            jdbcTemplate.update(
                    "DELETE FROM users WHERE id = ?",
                    userId
            );
        }

        createdUserIds.clear();

        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
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
    void createCard_shouldReturn400_whenUserAlreadyHasFiveActiveCards()
            throws Exception {
        long userId = createUser();

        createCard(userId, "1111222233334444");
        createCard(userId, "2222333344445555");
        createCard(userId, "3333444455556666");
        createCard(userId, "4444555566667777");
        createCard(userId, "5555666677778888");

        String sixthCardJson = """
                {
                  "number": "6666777788889999",
                  "holder": "IVAN IVANOV",
                  "expirationDate": "%s"
                }
                """.formatted(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sixthCardJson))
                .andExpect(status().isBadRequest());

        Integer cardCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_cards WHERE user_id = ? AND active = true",
                Integer.class,
                userId
        );

        assertEquals(
                5,
                cardCount,
                "После отклонения шестой карты у пользователя должно остаться пять активных карт"
        );
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
    void getCard_shouldReturn404_whenCardDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/cards/{cardId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
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
