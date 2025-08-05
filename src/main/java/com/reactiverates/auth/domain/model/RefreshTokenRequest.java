package com.reactiverates.auth.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на обновление токена")
public record RefreshTokenRequest(
    @Schema(description = "Refresh токен для получения нового access токена", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Refresh token не может быть пустым")
    String refreshToken
) { } 