package com.marketplace.userservice.controller;

import com.marketplace.userservice.dto.CardResponseDto;
import com.marketplace.userservice.dto.CreateCardRequest;
import com.marketplace.userservice.dto.UpdateCardRequest;
import com.marketplace.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class PaymentCardController {

    private final CardService cardService;

    @PostMapping("/api/users/{userId}/cards")
    public ResponseEntity<CardResponseDto> createCard(
            @PathVariable Long userId,
            @Valid @RequestBody CreateCardRequest request) {
        CardResponseDto created = cardService.createCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/users/{userId}/cards")
    public ResponseEntity<Page<CardResponseDto>> getCardsByUserId(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<CardResponseDto> cards = cardService.getCardsByUserId(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/api/cards/{cardId}")
    public ResponseEntity<CardResponseDto> getCardById(@PathVariable Long cardId) {
        CardResponseDto card = cardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }

    @PutMapping("/api/cards/{cardId}")
    public ResponseEntity<CardResponseDto> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request
            ) {
        CardResponseDto updated = cardService.updateCard(cardId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
