package com.reactiverates.auth.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с токенами аутентификации")
public record AuthResponse(
    @Schema(description = "JWT access токен для аутентификации", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    @Schema(description = "Refresh токен для обновления access токена", example = "550e8400-e29b-41d4-a716-446655440000")
    String refreshToken,
    @Schema(description = "Имя пользователя", example = "john_doe")
    String username,
    @Schema(description = "Email пользователя", example = "john.doe@example.com")
    String email
) { }
