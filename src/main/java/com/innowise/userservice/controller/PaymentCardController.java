package com.innowise.userservice.controller;

import com.innowise.userservice.dto.CardResponseDto;
import com.innowise.userservice.dto.CreateCardRequest;
import com.innowise.userservice.dto.UpdateCardRequest;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class PaymentCardController {

    private final CardService cardService;

    @PostMapping("/api/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponseDto> createCard(
            @PathVariable Long userId,
            @Valid @RequestBody CreateCardRequest request) {

        CardResponseDto created = cardService.createCard(userId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/cards/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/api/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardResponseDto>> getCards(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(
                cardService.getCardsByUserId(userId, pageable));
    }

    @GetMapping("/api/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @resourceAccessService.ownsCard(#cardId)")
    public ResponseEntity<CardResponseDto> getCard(@PathVariable Long cardId) {

        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @PutMapping("/api/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @resourceAccessService.ownsCard(#cardId)")
    public ResponseEntity<CardResponseDto> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request) {

        return ResponseEntity.ok(cardService.updateCard(cardId, request));
    }

    @DeleteMapping("/api/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @resourceAccessService.ownsCard(#cardId)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {

        cardService.deleteCard(cardId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/cards/{cardId}/status")
    @PreAuthorize("hasRole('ADMIN') or @resourceAccessService.ownsCard(#cardId)")
    public ResponseEntity<Void> changeCardStatus(
            @PathVariable Long cardId,
            @RequestParam boolean active) {

        cardService.changeCardStatus(cardId, active);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/users/me/cards")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponseDto> createMyCard(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCardRequest request) {

        Long userId = jwt.getClaim("user_id");

        CardResponseDto created = cardService.createCard(userId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/cards/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/api/users/me/cards")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<CardResponseDto>> getMyCards(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        Long userId = jwt.getClaim("user_id");

        return ResponseEntity.ok(
                cardService.getCardsByUserId(userId, pageable));
    }
}