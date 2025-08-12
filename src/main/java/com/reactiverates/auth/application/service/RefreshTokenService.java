package com.reactiverates.auth.application.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.reactiverates.auth.domain.exception.TokenException;
import com.reactiverates.auth.domain.model.UserDto;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;
import com.reactiverates.auth.infrastructure.persistance.entity.User;
import com.reactiverates.auth.infrastructure.persistance.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token.expiration:604800000}")
    private Long refreshTokenExpiration;
    
    public RefreshToken createRefreshToken(User user) {
        // Удаляем старый refresh token если существует
        refreshTokenRepository.findByUser(user).ifPresent(oldToken -> {
            refreshTokenRepository.delete(oldToken);
        });
        
        // Генерируем уникальный UUID для БД
        String tokenId = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(tokenId) // UUID в БД
            .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
            .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public RefreshToken createRefreshToken(UserDto grpcUser) {
        // Для GrpcUser мы создаем временный User entity для хранения в БД
        // Это необходимо для совместимости с существующей структурой БД
        User tempUser = User.builder()
            .id(grpcUser.getId())
            .username(grpcUser.getUsername())
            .email(grpcUser.getEmail())
            .password(grpcUser.getPassword())
            .role(User.Role.valueOf(grpcUser.getRole().name()))
            .build();
        
        return createRefreshToken(tempUser);
    }
    
    public String generateRefreshTokenJwt(User user, String tokenId) {
        return jwtService.generateRefreshToken(user, tokenId);
    }
    
    public String generateRefreshTokenJwt(UserDto grpcUser, String tokenId) {
        return jwtService.generateRefreshToken(grpcUser, tokenId);
    }
    
    public Optional<RefreshToken> findByTokenId(String tokenId) {
        return refreshTokenRepository.findByToken(tokenId);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
    
    public boolean deleteByUser(User user) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            refreshTokenRepository.deleteByUser(user);
            return true;
        }
        return false;
    }
    
    public boolean deleteByUser(UserDto grpcUser) {
        // Создаем временный User entity для поиска в БД
        User tempUser = User.builder()
            .id(grpcUser.getId())
            .username(grpcUser.getUsername())
            .email(grpcUser.getEmail())
            .password(grpcUser.getPassword())
            .role(User.Role.valueOf(grpcUser.getRole().name()))
            .build();
        
        return deleteByUser(tempUser);
    }
    
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }
} 