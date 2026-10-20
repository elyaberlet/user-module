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
import com.marketplace.userservice.specification.PaymentCardSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public CardResponseDto createCard(Long userId, CreateCardRequest request) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        long activeCardCount = paymentCardRepository.countByUserIdAndActiveTrue(userId);

        if (activeCardCount >= 5) {
            throw new CardLimitExceededException("User cannot have more than 5 active cards");
        }

        PaymentCard card = cardMapper.toEntity(request);
        card.setUser(user);
        PaymentCard saved = paymentCardRepository.save(card);
        return cardMapper.toResponseDto(saved);
    }

    public Page<CardResponseDto> getCardsByUserId(Long userId, Pageable pageable) {
        Specification<PaymentCard> spec = PaymentCardSpecification.hasUserId(userId);
        Page<PaymentCard> cards = paymentCardRepository.findAll(spec, pageable);
        return cards.map(cardMapper::toResponseDto);
    }

    @Cacheable(value = "cards", key = "#cardId")
    public CardResponseDto getCardById(Long cardId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new UserNotFoundException("Card not found with id: " + cardId));
        return cardMapper.toResponseDto(card);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#cardId")
    public CardResponseDto updateCard(Long cardId, UpdateCardRequest request) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new UserNotFoundException("Card not found with id: " + cardId));

        cardMapper.updateEntity(request, card);
        PaymentCard updated = paymentCardRepository.save(card);
        return cardMapper.toResponseDto(updated);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#cardId")
    public void deleteCard(Long cardId) {
        paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new UserNotFoundException("Card not found with id: " + cardId));
        paymentCardRepository.updateActiveStatus(cardId, false);
    }
}