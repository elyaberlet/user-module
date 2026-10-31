package com.innowise.userservice.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CardResponseDto> paymentCards
) implements Serializable {}
