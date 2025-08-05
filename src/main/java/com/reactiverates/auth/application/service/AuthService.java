package com.reactiverates.auth.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.reactiverates.auth.domain.exception.TokenException;
import com.reactiverates.auth.domain.model.AuthResponse;
import com.reactiverates.auth.domain.model.LoginRequest;
import com.reactiverates.auth.domain.model.LogoutResponse;
import com.reactiverates.auth.domain.model.RegisterRequest;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;
import com.reactiverates.auth.infrastructure.persistance.entity.User;
import com.reactiverates.auth.infrastructure.persistance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new TokenException("User already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) { 
            throw new TokenException("Email already exists");
        }

        var user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(User.Role.USER)
            .build();
        User savedUser = userRepository.save(user);
        
        var accessToken = jwtService.generateAccessToken(savedUser);
        var refreshTokenEntity = refreshTokenService.createRefreshToken(savedUser);
        var refreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(savedUser, refreshTokenEntity.getToken());
        
        return new AuthResponse(accessToken, refreshTokenJwt, savedUser.getUsername(), savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new TokenException("User not found"));

        var accessToken = jwtService.generateAccessToken(user);
        var refreshTokenEntity = refreshTokenService.createRefreshToken(user);
        var refreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(user, refreshTokenEntity.getToken());
        
        return new AuthResponse(accessToken, refreshTokenJwt, user.getUsername(), user.getEmail());
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
        
        User user = token.getUser();
        
        // 6. Генерируем новые токены
        var accessToken = jwtService.generateAccessToken(user);
        var newRefreshTokenEntity = refreshTokenService.createRefreshToken(user);
        var newRefreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(user, newRefreshTokenEntity.getToken());
        
        log.info("Token refreshed successfully for user: {}", username);
        
        return new AuthResponse(accessToken, newRefreshTokenJwt, user.getUsername(), user.getEmail());
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
        
        User user = token.getUser();
        
        log.debug("Logging out user: {}", username);
        
        // 5. Удаляем refresh token из базы данных
        boolean wasDeleted = refreshTokenService.deleteByUser(user);
        
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
