package com.marketplace.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpdateCardRequest {

    @Size(min = 16)
    private String number;

    private String holder;

    @Future
    private LocalDate expirationDate;
}
