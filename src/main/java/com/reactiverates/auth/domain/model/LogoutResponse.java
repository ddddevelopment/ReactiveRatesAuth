package com.reactiverates.auth.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ при выходе из системы")
public record LogoutResponse(
    @Schema(description = "Имя пользователя", example = "john_doe")
    String username,
    
    @Schema(description = "Сообщение об успешном выходе", example = "Successfully logged out")
    String message,
    
    @Schema(description = "Дополнительная информация", example = "All active sessions have been terminated")
    String details
) { } 