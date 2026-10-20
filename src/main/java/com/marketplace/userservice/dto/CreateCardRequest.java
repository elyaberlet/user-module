package com.marketplace.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CreateCardRequest {

    @NotBlank
    @Size(min = 16)
    private String number;

    @NotBlank
    private String holder;

    @NotBlank
    @Future
    private LocalDate expirationDate;
}
