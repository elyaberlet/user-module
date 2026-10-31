
package com.innowise.userservice.security;

import com.innowise.userservice.repository.PaymentCardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceAccessServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @InjectMocks
    private ResourceAccessService resourceAccessService;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("user_id", 123L)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownsCard_shouldReturnTrue_whenCardBelongsToCurrentUser() {
        when(cardRepository.existsByIdAndUser_Id(10L, 123L))
                .thenReturn(true);

        assertTrue(resourceAccessService.ownsCard(10L));

        verify(cardRepository).existsByIdAndUser_Id(10L, 123L);
    }

    @Test
    void ownsCard_shouldReturnFalse_whenCardBelongsToAnotherUser() {
        when(cardRepository.existsByIdAndUser_Id(10L, 123L))
                .thenReturn(false);

        assertFalse(resourceAccessService.ownsCard(10L));

        verify(cardRepository).existsByIdAndUser_Id(10L, 123L);
    }

    @Test
    void ownsCard_shouldReturnFalse_whenAuthenticationIsNotJwt() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password")
        );

        assertFalse(resourceAccessService.ownsCard(10L));

        verifyNoInteractions(cardRepository);
    }
}
