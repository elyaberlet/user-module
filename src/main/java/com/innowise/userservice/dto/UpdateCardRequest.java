
package com.innowise.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCardRequest(
        @Pattern(
                regexp = "\\d{16,19}",
                message = "Card number must contain 16 to 19 digits"
        )
        String number,

        @Size(max = 100)
        String holder,

        @Future
        LocalDate expirationDate
) {}
