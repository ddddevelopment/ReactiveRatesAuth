package com.reactiverates.auth.application.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.reactiverates.auth.domain.exception.TokenException;
import com.reactiverates.auth.domain.model.UserDto;
import com.reactiverates.auth.domain.service.RefreshTokenService;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;
import com.reactiverates.auth.infrastructure.persistance.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultRefreshTokenService implements RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token.expiration:604800000}")
    private Long refreshTokenExpiration;
    
    @Override
	public RefreshToken createRefreshToken(UserDto userDto) {
        // Удаляем старый refresh token если существует
        refreshTokenRepository.findByUserId(userDto.getId()).ifPresent(oldToken -> {
            refreshTokenRepository.delete(oldToken);
        });
        
        // Генерируем уникальный UUID для БД
        String tokenId = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
            .userId(userDto.getId()) // Храним только ID пользователя
            .token(tokenId) // UUID в БД
            .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
            .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    @Override
	public String generateRefreshTokenJwt(UserDto userDto, String tokenId) {
        return jwtService.generateRefreshToken(userDto, tokenId);
    }
    
    @Override
	public Optional<RefreshToken> findByTokenId(String tokenId) {
        return refreshTokenRepository.findByToken(tokenId);
    }
    
    @Override
	public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
    
    @Override
	public boolean deleteByUser(UserDto userDto) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(userDto.getId());
        if (existingToken.isPresent()) {
            refreshTokenRepository.deleteByUserId(userDto.getId());
            return true;
        }
        return false;
    }
    
    @Override
	public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }
} 