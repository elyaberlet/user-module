package com.marketplace.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpdateUserRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String surname;

    private LocalDate birthDate;

    @Email
    private String email;
}
