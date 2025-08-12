package com.reactiverates.auth.domain.service;

import java.util.Optional;

import com.reactiverates.auth.domain.model.UserDto;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;
import com.reactiverates.auth.infrastructure.persistance.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken createRefreshToken(UserDto grpcUser);

    String generateRefreshTokenJwt(User user, String tokenId);

    String generateRefreshTokenJwt(UserDto grpcUser, String tokenId);

    Optional<RefreshToken> findByTokenId(String tokenId);

    RefreshToken verifyExpiration(RefreshToken token);

    boolean deleteByUser(User user);

    boolean deleteByUser(UserDto grpcUser);

    void deleteExpiredTokens();

}