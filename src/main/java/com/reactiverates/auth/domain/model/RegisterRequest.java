package com.reactiverates.auth.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на регистрацию нового пользователя")
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "Имя пользователя", example = "john_doe")
    private String username;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Schema(description = "Email пользователя", example = "john.doe@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Пароль пользователя", example = "password123")
    private String password;

    @Schema(description = "Имя пользователя", example = "John")
    private String firstName;

    @Schema(description = "Фамилия пользователя", example = "Doe")
    private String lastName;

    @Schema(description = "Номер телефона пользователя", example = "+1234567890")
    private String phoneNumber;
}
