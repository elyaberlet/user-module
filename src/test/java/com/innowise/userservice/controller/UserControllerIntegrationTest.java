package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.config.SecurityConfig;
import com.innowise.userservice.AbstractIntegrationTest;
import com.innowise.userservice.dto.CreateUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

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
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

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

        long userId = objectMapper.readTree(response).get("id").asLong();
        createdUserIds.add(userId);

        return userId;
    }

    private void createUserJsonAndSave(String name, String email)
            throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(name, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long userId = objectMapper.readTree(response).get("id").asLong();
        createdUserIds.add(userId);
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

        createdUserIds.add(userId);

        String location = result.getResponse().getHeader("Location");

        assertNotNull(location, "Заголовок Location должен присутствовать");
        assertTrue(
                location.endsWith("/api/users/" + userId),
                "Location должен содержать ID созданного пользователя"
        );
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        String email = "duplicate@example.com";

        createUser(email);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(email)))
                .andExpect(status().isConflict());
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
                .andExpect(jsonPath("$.email").value("get-user@example.com"))
                .andExpect(jsonPath("$.paymentCards").doesNotExist());
    }

    @Test
    void getUser_shouldReturn404_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
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
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[0].paymentCards").doesNotExist())
                .andExpect(jsonPath("$.content[1].paymentCards").doesNotExist());
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
                        .value("filter-ivan@example.com"))
                .andExpect(jsonPath("$.content[0].paymentCards").doesNotExist());
    }

    @Test
    void getMyProfile_shouldReturnUserWithCards() throws Exception {
        long userId = createUser("my-profile@example.com");

        mockMvc.perform(get("/api/users/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .claim("role", "USER")
                                        .claim("user_id", userId))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.email").value("my-profile@example.com"))
                .andExpect(jsonPath("$.paymentCards").isArray());
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
    void deleteUser_shouldReturn204AndRemoveUserAndCards() throws Exception {
        long userId = createUser("delete-user@example.com");

        createCard(userId, "1111222233334444");
        createCard(userId, "2222333344445555");

        assertEquals(
                2,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM payment_cards WHERE user_id = ?",
                        Integer.class,
                        userId
                )
        );

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());

        Integer remainingUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId
        );

        Integer remainingCards = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_cards WHERE user_id = ?",
                Integer.class,
                userId
        );

        assertEquals(0, remainingUsers, "Пользователь должен быть удалён из БД");
        assertEquals(0, remainingCards, "Карты пользователя должны быть удалены");
    }

    @Test
    void cache_shouldReturnCachedUserAndEvictAfterUpdate() throws Exception {
        long userId = createUser("cache-user@example.com");

        Cache usersCache = cacheManager.getCache("users");
        assertNotNull(usersCache, "Кэш users должен существовать");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ivan"));

        assertNotNull(
                usersCache.get(userId),
                "Пользователь должен попасть в кэш после первого запроса"
        );

        jdbcTemplate.update(
                "UPDATE users SET name = ? WHERE id = ?",
                "DatabaseChanged",
                userId
        );

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ivan"));

        String updateJson = """
                {
                  "name": "Petr",
                  "surname": "Petrov",
                  "birthDate": "1994-03-15",
                  "email": "cache-user@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Petr"));

        assertNull(
                usersCache.get(userId),
                "Старая запись пользователя должна быть удалена из кэша users"
        );

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Petr"));
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

    @Test
    void jwt_shouldContainAdminAuthority() {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("role", "ADMIN")
                .claim("user_id", 1L)
                .build();

        var converter = new SecurityConfig()
                .jwtAuthenticationConverter();

        var authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertTrue(
                authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_ADMIN")),
                "JWT с role=ADMIN должен создавать authority ROLE_ADMIN"
        );
    }

    @Test
    void userRoleShouldNotAccessAdminUserList() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt()
                                .jwt(token -> token
                                        .claim("role", "USER")
                                        .claim("user_id", 1L))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleShouldNotCreateUser() throws Exception {
        String requestBody = """
                {
                    "name": "Alice",
                    "surname": "Smith",
                    "birthDate": "1995-05-15",
                    "email": "alice-security-test@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(jwt()
                                .jwt(token -> token
                                        .claim("role", "USER")
                                        .claim("user_id", 1L))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleShouldNotChangeUserStatus() throws Exception {
        mockMvc.perform(patch("/api/users/{id}/status", 2L)
                        .param("active", "false")
                        .with(jwt()
                                .jwt(token -> token
                                        .claim("role", "USER")
                                        .claim("user_id", 1L))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleShouldNotAccessAnotherUsersProfile() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 2L)
                        .with(jwt()
                                .jwt(token -> token
                                        .claim("role", "USER")
                                        .claim("user_id", 1L))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )))
                .andExpect(status().isForbidden());
    }
}
