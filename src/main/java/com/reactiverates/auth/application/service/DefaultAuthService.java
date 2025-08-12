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
import com.reactiverates.auth.domain.service.AuthService;
import com.reactiverates.auth.domain.service.RefreshTokenService;
import com.reactiverates.auth.domain.service.UsersService;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultAuthService implements AuthService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UsersService usersService;

    @Override
	public AuthResponse register(RegisterRequest request) {
        Optional<UserDto> existingUser = usersService.getUserByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            throw new TokenException("User already exists");
        }
        
        log.info("User not found in gRPC service, proceeding with registration: {}", request.getUsername());

        try {
            UserDto userDto = usersService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName() != null ? request.getFirstName() : "",
                request.getLastName() != null ? request.getLastName() : "",
                request.getPhoneNumber() != null ? request.getPhoneNumber() : ""
            );
            
            log.info("User created successfully via gRPC: {}", request.getUsername());
            
            var accessToken = jwtService.generateAccessToken(userDto);
            var refreshTokenEntity = refreshTokenService.createRefreshToken(userDto);
            var refreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(userDto, refreshTokenEntity.getToken());

            return new AuthResponse(accessToken, refreshTokenJwt, userDto.getUsername(), userDto.getEmail());
        } catch (Exception e) {
            log.error("Failed to create user via gRPC: {}", e.getMessage(), e);
            throw new TokenException("Failed to create user: " + e.getMessage());
        }
    }

    @Override
	public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        Optional<UserDto> userDtoOptional = usersService.getUserByUsername(request.getUsername());
        if (userDtoOptional.isEmpty()) {
            throw new TokenException("User not found: " + request.getUsername());
        }
        UserDto userDto = userDtoOptional.get();

        var accessToken = jwtService.generateAccessToken(userDto);
        var refreshTokenEntity = refreshTokenService.createRefreshToken(userDto);
        var refreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(userDto, refreshTokenEntity.getToken());
        
        return new AuthResponse(accessToken, refreshTokenJwt, userDto.getUsername(), userDto.getEmail());
    }

    @Override
	public AuthResponse refreshToken(String refreshTokenJwt) {
        log.info("Attempting to refresh token");
        
        if (!jwtService.isRefreshToken(refreshTokenJwt)) {
            throw new TokenException("Invalid refresh token type");
        }
        
        String tokenId = jwtService.extractTokenId(refreshTokenJwt);
        String username = jwtService.extractUsername(refreshTokenJwt);
        
        // Проверяем существование токена в БД
        RefreshToken token = refreshTokenService.findByTokenId(tokenId)
            .orElseThrow(() -> new TokenException("Refresh token not found in database"));
        
        // Проверяем срок действия
        token = refreshTokenService.verifyExpiration(token);
        
        // Получаем актуальные данные пользователя из gRPC сервиса
        Optional<UserDto> userDtoOptional = usersService.getUserByUsername(username);
        if (userDtoOptional.isEmpty()) {
            throw new TokenException("User not found: " + username);
        }
        UserDto userDto = userDtoOptional.get();
        
        // Проверяем, что токен принадлежит правильному пользователю
        if (!token.getUserId().equals(userDto.getId())) {
            throw new TokenException("Token user mismatch");
        }
        
        var accessToken = jwtService.generateAccessToken(userDto);
        var newRefreshTokenEntity = refreshTokenService.createRefreshToken(userDto);
        var newRefreshTokenJwt = refreshTokenService.generateRefreshTokenJwt(userDto, newRefreshTokenEntity.getToken());
        
        log.info("Token refreshed successfully for user: {}", username);
        
        return new AuthResponse(accessToken, newRefreshTokenJwt, userDto.getUsername(), userDto.getEmail());
    }

    @Override
	public LogoutResponse logout(String refreshTokenJwt) {
        log.info("Attempting logout with refresh token");
        
        if (!jwtService.isRefreshToken(refreshTokenJwt)) {
            throw new TokenException("Invalid refresh token type");
        }
        
        String tokenId = jwtService.extractTokenId(refreshTokenJwt);
        String username = jwtService.extractUsername(refreshTokenJwt);
        
        RefreshToken token = refreshTokenService.findByTokenId(tokenId)
            .orElseThrow(() -> new TokenException("Refresh token not found or already invalid"));
        
        Optional<UserDto> userDtoOptional = usersService.getUserByUsername(username);
        if (userDtoOptional.isEmpty()) {
            throw new TokenException("User not found: " + username);
        }
        UserDto userDto = userDtoOptional.get();
        
        // Проверяем, что токен принадлежит правильному пользователю
        if (!token.getUserId().equals(userDto.getId())) {
            throw new TokenException("Token user mismatch");
        }
        
        log.debug("Logging out user: {}", username);
        
        boolean wasDeleted = refreshTokenService.deleteByUser(userDto);
        
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
