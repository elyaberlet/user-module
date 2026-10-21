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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {
    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    @Transactional
    @CacheEvict(cacheNames = "users", key = "#userId")
    public CardResponseDto createCard(Long userId, CreateCardRequest request) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (cardRepository.countByUser_IdAndActiveTrue(userId) >= 5) {
            throw new CardLimitExceededException(userId);
        }
        PaymentCard card = cardMapper.toEntity(request);
        card.setUser(user);
        return cardMapper.toDto(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<CardResponseDto> getCardsByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) throw new UserNotFoundException(userId);
        return cardRepository.findByUser_IdAndActiveTrue(userId, pageable).map(cardMapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "cards", key = "#cardId")
    public CardResponseDto getCardById(Long cardId) {
        return cardMapper.toDto(cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId)));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "cards", key = "#cardId"),
            @CacheEvict(cacheNames = "users", allEntries = true)
    })
    public CardResponseDto updateCard(Long cardId, UpdateCardRequest request) {
        PaymentCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        cardMapper.updateEntity(request, card);
        return cardMapper.toDto(cardRepository.save(card));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "cards", key = "#cardId"),
            @CacheEvict(cacheNames = "users", allEntries = true)
    })
    public void deleteCard(Long cardId) {
        PaymentCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        cardRepository.delete(card);
    }
}
