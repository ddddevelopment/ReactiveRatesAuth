package com.reactiverates.auth.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на аутентификацию пользователя")
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "Имя пользователя", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Пароль пользователя", example = "password123")
    private String password;
}
