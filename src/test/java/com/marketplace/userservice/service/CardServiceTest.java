package com.marketplace.userservice.service;

import com.marketplace.userservice.dto.CardResponseDto;
import com.marketplace.userservice.dto.CreateCardRequest;
import com.marketplace.userservice.dto.UpdateCardRequest;
import com.marketplace.userservice.entity.PaymentCard;
import com.marketplace.userservice.entity.User;
import com.marketplace.userservice.exception.CardLimitExceededException;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private CreateCardRequest createValidRequest() {
        return CreateCardRequest.builder()
                .number("1234567890123456")
                .holder("IVAN IVANOV")
                .expirationDate(LocalDate.now().plusYears(1))
                .build();
    }

    private UpdateCardRequest updateValidRequest() {
        return UpdateCardRequest.builder()
                .number("9876543210987654")
                .holder("PETR PETROV")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();
    }

    private PaymentCard createMockCard(Long cardId, Long userId) {
        User mockUser = new User();
        mockUser.setId(userId);
        return PaymentCard.builder()
                .id(cardId)
                .user(mockUser)
                .number("1234567890123456")
                .holder("IVAN IVANOV")
                .expirationDate(LocalDate.now().plusYears(1))
                .active(true)
                .build();
    }

    @Test
    void shouldCreateCardSuccessfullyWhenLimitNotReached() {
        Long userId = 1L;
        CreateCardRequest request = createValidRequest();
        User mockUser = new User();
        mockUser.setId(userId);
        PaymentCard mockCard = createMockCard(1L, userId);
        CardResponseDto expectedDto = CardResponseDto.builder().id(1L).number("1234567890123456").build();

        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(mockUser));
        when(paymentCardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(2L);
        when(cardMapper.toEntity(request)).thenReturn(mockCard);
        when(paymentCardRepository.save(mockCard)).thenReturn(mockCard);
        when(cardMapper.toResponseDto(mockCard)).thenReturn(expectedDto);

        CardResponseDto result = cardService.createCard(userId, request);

        assertEquals(expectedDto, result);
        verify(userRepository, times(1)).findByIdAndActiveTrue(userId);
        verify(paymentCardRepository, times(1)).countByUserIdAndActiveTrue(userId);
        verify(paymentCardRepository, times(1)).save(mockCard);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringCardCreation() {
        Long userId = 999L;
        CreateCardRequest request = createValidRequest();

        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.createCard(userId, request));
        verify(paymentCardRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCardLimitExceeded() {
        Long userId = 1L;
        CreateCardRequest request = createValidRequest();
        User mockUser = new User();
        mockUser.setId(userId);

        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(mockUser));
        when(paymentCardRepository.countByUserIdAndActiveTrue(userId)).thenReturn(5L);

        assertThrows(CardLimitExceededException.class, () -> cardService.createCard(userId, request));
        verify(paymentCardRepository, never()).save(any());
    }

    @Test
    void shouldReturnPageOfCardsByUserId() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PaymentCard mockCard = createMockCard(1L, userId);
        CardResponseDto dto = CardResponseDto.builder().id(1L).build();

        when(paymentCardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(mockCard), pageable, 1));
        when(cardMapper.toResponseDto(mockCard)).thenReturn(dto);

        Page<CardResponseDto> result = cardService.getCardsByUserId(userId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(dto, result.getContent().get(0));
        verify(paymentCardRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnCardById() {
        Long cardId = 1L;
        PaymentCard mockCard = createMockCard(cardId, 1L);
        CardResponseDto expectedDto = CardResponseDto.builder().id(cardId).number("1234567890123456").build();

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(mockCard));
        when(cardMapper.toResponseDto(mockCard)).thenReturn(expectedDto);

        CardResponseDto result = cardService.getCardById(cardId);

        assertEquals(expectedDto, result);
        verify(paymentCardRepository, times(1)).findById(cardId);
    }

    @Test
    void shouldThrowExceptionWhenCardNotFoundById() {
        Long cardId = 999L;

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.getCardById(cardId));
    }

    @Test
    void shouldUpdateCardSuccessfully() {
        Long cardId = 1L;
        UpdateCardRequest request = updateValidRequest();
        PaymentCard mockCard = createMockCard(cardId, 1L);
        CardResponseDto expectedDto = CardResponseDto.builder().id(cardId).number("9876543210987654").build();

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(mockCard));
        doNothing().when(cardMapper).updateEntity(request, mockCard);
        when(paymentCardRepository.save(mockCard)).thenReturn(mockCard);
        when(cardMapper.toResponseDto(mockCard)).thenReturn(expectedDto);

        CardResponseDto result = cardService.updateCard(cardId, request);

        assertEquals(expectedDto, result);
        verify(paymentCardRepository, times(1)).findById(cardId);
        verify(paymentCardRepository, times(1)).save(mockCard);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentCard() {
        Long cardId = 999L;
        UpdateCardRequest request = updateValidRequest();

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.updateCard(cardId, request));
    }

    @Test
    void shouldDeactivateCardWhenDeleted() {
        Long cardId = 1L;
        PaymentCard mockCard = createMockCard(cardId, 1L);

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(mockCard));
        doNothing().when(paymentCardRepository).updateActiveStatus(cardId, false);

        cardService.deleteCard(cardId);

        verify(paymentCardRepository, times(1)).findById(cardId);
        verify(paymentCardRepository, times(1)).updateActiveStatus(cardId, false);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentCard() {
        Long cardId = 999L;

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.deleteCard(cardId));
    }
}