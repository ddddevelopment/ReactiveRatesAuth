package com.reactiverates.auth.api.rest.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reactiverates.auth.infrastructure.persistance.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/user")
@Tag(name = "User Management", description = "API для управления профилем пользователя")
public class UserController {

    @GetMapping("/profile")
    @Operation(
        summary = "Получить профиль пользователя",
        description = "Возвращает информацию о текущем аутентифицированном пользователе"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Профиль пользователя успешно получен",
            content = @Content(schema = @Schema(implementation = User.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        )
    })
    public ResponseEntity<User> getProfile(Authentication authentication) {
        return ResponseEntity.ok((User) authentication.getPrincipal());
    }
}