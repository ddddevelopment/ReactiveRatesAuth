package com.reactiverates.auth.api.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reactiverates.auth.application.service.AuthService;
import com.reactiverates.auth.domain.model.AuthResponse;
import com.reactiverates.auth.domain.model.LoginRequest;
import com.reactiverates.auth.domain.model.RefreshTokenRequest;
import com.reactiverates.auth.domain.model.RegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для аутентификации и регистрации пользователей")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
        summary = "Регистрация нового пользователя",
        description = "Создает нового пользователя в системе и возвращает access и refresh токены"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь успешно зарегистрирован",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Пользователь с таким именем или email уже существует"
        )
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Аутентификация пользователя",
        description = "Аутентифицирует пользователя по логину и паролю, возвращает access и refresh токены"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешная аутентификация",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Неверные учетные данные"
        )
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Обновление токена",
        description = "Обновляет access токен используя refresh токен"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токен успешно обновлен",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Недействительный или истекший refresh токен"
        )
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken()));
    }

    @DeleteMapping("/logout")
    @Operation(
        summary = "Выход из системы",
        description = "Удаляет refresh токен пользователя, завершая сессию"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешный выход из системы"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса"
        )
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}