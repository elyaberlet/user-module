
package com.marketplace.userservice.service;

import com.marketplace.userservice.dto.CardResponseDto;
import com.marketplace.userservice.dto.CreateCardRequest;
import com.marketplace.userservice.dto.UpdateCardRequest;
import com.marketplace.userservice.entity.PaymentCard;
import com.marketplace.userservice.entity.User;
import com.marketplace.userservice.exception.CardLimitExceededException;
import com.marketplace.userservice.exception.CardNotFoundException;
import com.marketplace.userservice.exception.UserNotFoundException;
import com.marketplace.userservice.mapper.PaymentCardMapper;
import com.marketplace.userservice.repository.PaymentCardRepository;
import com.marketplace.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setEmail("ivan@test.com");
        user.setActive(true);
        return user;
    }

    private PaymentCard createCard(Long id, User user) {
        PaymentCard card = new PaymentCard();
        card.setId(id);
        card.setUser(user);
        card.setNumber("1234567890123456");
        card.setHolder("IVAN IVANOV");
        card.setExpirationDate(LocalDate.of(2028, 12, 31));
        card.setActive(true);
        return card;
    }

    private CardResponseDto createCardResponse(Long id) {
        return new CardResponseDto(
                id,
                "1234567890123456",
                "IVAN IVANOV",
                LocalDate.of(2028, 12, 31),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createCard_shouldCreateCard() {
        Long userId = 1L;

        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456",
                "IVAN IVANOV",
                LocalDate.of(2028, 12, 31)
        );

        User user = createUser(userId);
        PaymentCard card = createCard(10L, user);
        CardResponseDto response = createCardResponse(10L);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(cardRepository.countByUser_IdAndActiveTrue(userId))
                .thenReturn(0L);
        when(cardMapper.toEntity(request)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(response);

        CardResponseDto result = cardService.createCard(userId, request);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("1234567890123456", result.number());

        verify(cardRepository).save(card);
        verify(cardMapper).toDto(card);
    }

    @Test
    void createCard_shouldAllowFifthActiveCard() {
        Long userId = 1L;

        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456",
                "IVAN IVANOV",
                LocalDate.of(2028, 12, 31)
        );

        User user = createUser(userId);
        PaymentCard card = createCard(10L, user);
        CardResponseDto response = createCardResponse(10L);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(cardRepository.countByUser_IdAndActiveTrue(userId))
                .thenReturn(4L);
        when(cardMapper.toEntity(request)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(response);

        CardResponseDto result = cardService.createCard(userId, request);

        assertEquals(10L, result.id());
        verify(cardRepository).save(card);
    }

    @Test
    void createCard_shouldThrowWhenUserNotFound() {
        Long userId = 99L;

        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456",
                "IVAN IVANOV",
                LocalDate.of(2028, 12, 31)
        );

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> cardService.createCard(userId, request)
        );

        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void createCard_shouldThrowWhenCardLimitExceeded() {
        Long userId = 1L;

        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456",
                "IVAN IVANOV",
                LocalDate.of(2028, 12, 31)
        );

        User user = createUser(userId);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(cardRepository.countByUser_IdAndActiveTrue(userId))
                .thenReturn(5L);

        assertThrows(
                CardLimitExceededException.class,
                () -> cardService.createCard(userId, request)
        );

        verify(cardRepository, never()).save(any(PaymentCard.class));
        verify(cardMapper, never()).toEntity(any(CreateCardRequest.class));
    }

    @Test
    void getCardsByUserId_shouldReturnPageOfCards() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = createUser(userId);
        PaymentCard card = createCard(10L, user);
        CardResponseDto response = createCardResponse(10L);

        Page<PaymentCard> cardPage = new PageImpl<>(List.of(card));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findByUser_IdAndActiveTrue(userId, pageable))
                .thenReturn(cardPage);
        when(cardMapper.toDto(card)).thenReturn(response);

        Page<CardResponseDto> result =
                cardService.getCardsByUserId(userId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).id());

        verify(cardRepository).findByUser_IdAndActiveTrue(userId, pageable);
    }

    @Test
    void getCardsByUserId_shouldThrowWhenUserNotFound() {
        Long userId = 99L;
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> cardService.getCardsByUserId(userId, pageable)
        );

        verify(cardRepository, never())
                .findByUser_IdAndActiveTrue(anyLong(), any(Pageable.class));
    }

    @Test
    void getCardById_shouldReturnCard() {
        Long cardId = 10L;
        PaymentCard card = createCard(cardId, createUser(1L));
        CardResponseDto response = createCardResponse(cardId);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toDto(card)).thenReturn(response);

        CardResponseDto result = cardService.getCardById(cardId);

        assertNotNull(result);
        assertEquals(cardId, result.id());
        assertEquals("IVAN IVANOV", result.holder());

        verify(cardMapper).toDto(card);
    }

    @Test
    void getCardById_shouldThrowWhenCardNotFound() {
        Long cardId = 99L;

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(
                CardNotFoundException.class,
                () -> cardService.getCardById(cardId)
        );

        verify(cardMapper, never()).toDto(any(PaymentCard.class));
    }

    @Test
    void updateCard_shouldUpdateAndReturnCard() {
        Long cardId = 10L;

        UpdateCardRequest request = UpdateCardRequest.builder()
                .holder("NEW HOLDER")
                .build();

        PaymentCard card = createCard(cardId, createUser(1L));
        CardResponseDto response = new CardResponseDto(
                cardId,
                card.getNumber(),
                "NEW HOLDER",
                card.getExpirationDate(),
                true,
                null,
                null
        );

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(response);

        CardResponseDto result = cardService.updateCard(cardId, request);

        assertEquals(cardId, result.id());
        assertEquals("NEW HOLDER", result.holder());

        verify(cardMapper).updateEntity(request, card);
        verify(cardRepository).save(card);
    }

    @Test
    void updateCard_shouldThrowWhenCardNotFound() {
        Long cardId = 99L;

        UpdateCardRequest request = UpdateCardRequest.builder()
                .holder("NEW HOLDER")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(
                CardNotFoundException.class,
                () -> cardService.updateCard(cardId, request)
        );

        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void deleteCard_shouldDeleteCard() {
        Long cardId = 10L;
        PaymentCard card = createCard(cardId, createUser(1L));

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertDoesNotThrow(() -> cardService.deleteCard(cardId));

        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_shouldThrowWhenCardNotFound() {
        Long cardId = 99L;

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(
                CardNotFoundException.class,
                () -> cardService.deleteCard(cardId)
        );

        verify(cardRepository, never()).delete(any(PaymentCard.class));
    }
}
