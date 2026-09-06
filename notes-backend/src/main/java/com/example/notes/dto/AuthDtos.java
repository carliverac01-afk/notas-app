package com.example.notes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            String tokenType,
            String accessToken,
            long expiresInSeconds,
            UserResponse user
    ) {
    }

    public record UserResponse(Long id, String email, String name) {
    }
}
