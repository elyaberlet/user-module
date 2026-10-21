package com.marketplace.userservice.controller;

import com.marketplace.userservice.dto.CardResponseDto;
import com.marketplace.userservice.dto.CreateCardRequest;
import com.marketplace.userservice.dto.UpdateCardRequest;
import com.marketplace.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class PaymentCardController {
    private final CardService cardService;

    @PostMapping("/api/users/{userId}/cards")
    public ResponseEntity<CardResponseDto> createCard(@PathVariable Long userId, @Valid @RequestBody CreateCardRequest request) {
        CardResponseDto created = cardService.createCard(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/cards/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/api/users/{userId}/cards")
    public ResponseEntity<Page<CardResponseDto>> getCards(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(cardService.getCardsByUserId(userId, pageable));
    }

    @GetMapping("/api/cards/{cardId}")
    public ResponseEntity<CardResponseDto> getCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @PutMapping("/api/cards/{cardId}")
    public ResponseEntity<CardResponseDto> updateCard(@PathVariable Long cardId, @Valid @RequestBody UpdateCardRequest request) {
        return ResponseEntity.ok(cardService.updateCard(cardId, request));
    }

    @DeleteMapping("/api/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
