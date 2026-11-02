package com.innowise.userservice.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CardResponseDto(
        Long id,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {}
