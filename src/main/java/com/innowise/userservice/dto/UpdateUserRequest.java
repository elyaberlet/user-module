package com.innowise.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserRequest(
        @Size(min = 1, max = 100) String name,
        @Size(min = 1, max = 100) String surname,
        @Past LocalDate birthDate,
        @Email @Size(max = 255) String email
) {}
