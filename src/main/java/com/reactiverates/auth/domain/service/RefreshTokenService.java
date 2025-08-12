package com.reactiverates.auth.domain.service;

import java.util.Optional;

import com.reactiverates.auth.domain.model.UserDto;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(UserDto userDto);

    String generateRefreshTokenJwt(UserDto userDto, String tokenId);

    Optional<RefreshToken> findByTokenId(String tokenId);

    RefreshToken verifyExpiration(RefreshToken token);

    boolean deleteByUser(UserDto userDto);

    void deleteExpiredTokens();

}