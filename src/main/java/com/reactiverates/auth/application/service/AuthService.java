package com.reactiverates.auth.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.reactiverates.auth.domain.exception.TokenException;
import com.reactiverates.auth.domain.model.AuthResponse;
import com.reactiverates.auth.domain.model.UserDto;
import com.reactiverates.auth.domain.model.LoginRequest;
import com.reactiverates.auth.domain.model.LogoutResponse;
import com.reactiverates.auth.domain.model.RegisterRequest;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UsersGrpcClient usersGrpcClient;

    public AuthResponse register(RegisterRequest request) {
        // Проверяем существование пользователя через gRPC сервис
        try {
            usersGrpcClient.getUserByUsername(request.getUsername());
            throw new TokenException("User already exists");
        } catch (Exception e) {
            // Пользователь не найден, продолжаем регистрацию
            log.info("User not found in gRPC service, proceeding with registration: {}", request.getUsername());
        }

        // Создаем пользователя через gRPC сервис
        try {
            usersGrpcClient.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName() != null ? request.getFirstName() : "",
                request.getLastName() != null ? request.getLastName() : "",
                request.getPhoneNumber() != null ? request.getPhoneNumber() : ""
            );
            
            log.info("User created successfully via gRPC: {}", request.getUsername());
        } catch (Exception e) {
            log.error("Failed to create user via gRPC: {}", e.getMessage(), e);
            throw new TokenException("Failed to create user: " + e.getMessage());
        }

        // Получаем созданного пользователя для генерации токенов
        UserDto userDto = usersGrpcClient.getGrpcUserByUsername(request.getUsername());
        
        var accessToken = jwtService.generateAccessToken(userDto);
        var refreshTokenEntity = refreshTokenService.createRefreshToken(userDto);
        var refreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(userDto, refreshTokenEntity.getToken());
        
        return new AuthResponse(accessToken, refreshTokenJwt, userDto.getUsername(), userDto.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        // Аутентификация через Spring Security (использует CustomUserDetailsService с gRPC)
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Получаем пользователя через gRPC сервис
        UserDto grpcUser = usersGrpcClient.getGrpcUserByUsername(request.getUsername());

        var accessToken = jwtService.generateAccessToken(grpcUser);
        var refreshTokenEntity = refreshTokenService.createRefreshToken(grpcUser);
        var refreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(grpcUser, refreshTokenEntity.getToken());
        
        return new AuthResponse(accessToken, refreshTokenJwt, grpcUser.getUsername(), grpcUser.getEmail());
    }

    public AuthResponse refreshToken(String refreshTokenJwt) {
        log.info("Attempting to refresh token");
        
        // 1. Валидируем JWT
        if (!jwtService.isRefreshToken(refreshTokenJwt)) {
            throw new TokenException("Invalid refresh token type");
        }
        
        // 2. Извлекаем tokenId из JWT
        String tokenId = jwtService.extractTokenId(refreshTokenJwt);
        String username = jwtService.extractUsername(refreshTokenJwt);
        
        // 3. Находим токен в БД
        RefreshToken token = refreshTokenService.findByTokenId(tokenId)
            .orElseThrow(() -> new TokenException("Refresh token not found in database"));
        
        // 4. Проверяем пользователя
        if (!token.getUser().getUsername().equals(username)) {
            throw new TokenException("Token user mismatch");
        }
        
        // 5. Проверяем срок действия
        token = refreshTokenService.verifyExpiration(token);
        
        // Получаем актуальные данные пользователя через gRPC
        UserDto grpcUser = usersGrpcClient.getGrpcUserByUsername(username);
        
        // 6. Генерируем новые токены
        var accessToken = jwtService.generateAccessToken(grpcUser);
        var newRefreshTokenEntity = refreshTokenService.createRefreshToken(grpcUser);
        var newRefreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(grpcUser, newRefreshTokenEntity.getToken());
        
        log.info("Token refreshed successfully for user: {}", username);
        
        return new AuthResponse(accessToken, newRefreshTokenJwt, grpcUser.getUsername(), grpcUser.getEmail());
    }

    public LogoutResponse logout(String refreshTokenJwt) {
        log.info("Attempting logout with refresh token");
        
        // 1. Валидируем JWT
        if (!jwtService.isRefreshToken(refreshTokenJwt)) {
            throw new TokenException("Invalid refresh token type");
        }
        
        // 2. Извлекаем tokenId из JWT
        String tokenId = jwtService.extractTokenId(refreshTokenJwt);
        String username = jwtService.extractUsername(refreshTokenJwt);
        
        // 3. Находим токен в БД
        RefreshToken token = refreshTokenService.findByTokenId(tokenId)
            .orElseThrow(() -> new TokenException("Refresh token not found or already invalid"));
        
        // 4. Проверяем пользователя
        if (!token.getUser().getUsername().equals(username)) {
            throw new TokenException("Token user mismatch");
        }
        
        // Получаем актуальные данные пользователя через gRPC
        UserDto grpcUser = usersGrpcClient.getGrpcUserByUsername(username);
        
        log.debug("Logging out user: {}", username);
        
        // 5. Удаляем refresh token из базы данных
        boolean wasDeleted = refreshTokenService.deleteByUser(grpcUser);
        
        String message = wasDeleted 
            ? "Successfully logged out" 
            : "User was already logged out";
        
        String details = wasDeleted 
            ? "All active sessions have been terminated" 
            : "No active sessions found for this user";
        
        log.info("User {} logout completed. Session deleted: {}", username, wasDeleted);
        
        return new LogoutResponse(username, message, details);
    }
}
