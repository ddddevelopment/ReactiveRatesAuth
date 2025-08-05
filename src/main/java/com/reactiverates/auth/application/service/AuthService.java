package com.reactiverates.auth.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.reactiverates.auth.domain.model.AuthResponse;
import com.reactiverates.auth.domain.model.LoginRequest;
import com.reactiverates.auth.domain.model.RegisterRequest;
import com.reactiverates.auth.infrastructure.persistance.entity.RefreshToken;
import com.reactiverates.auth.infrastructure.persistance.entity.User;
import com.reactiverates.auth.infrastructure.persistance.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) { 
            throw new RuntimeException("Email already exists");
        }

        var user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(User.Role.USER)
            .build();
        User savedUser = userRepository.save(user);
        
        var accessToken = jwtService.generateAccessToken(savedUser);
        var refreshToken = refreshTokenService.createRefreshToken(savedUser);
        
        return new AuthResponse(accessToken, refreshToken.getToken(), savedUser.getUsername(), savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user);
        
        return new AuthResponse(accessToken, refreshToken.getToken(), user.getUsername(), user.getEmail());
    }

    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        
        token = refreshTokenService.verifyExpiration(token);
        
        User user = token.getUser();
        var accessToken = jwtService.generateAccessToken(user);
        var newRefreshToken = refreshTokenService.createRefreshToken(user);
        
        return new AuthResponse(accessToken, newRefreshToken.getToken(), user.getUsername(), user.getEmail());
    }

    public void logout(String refreshToken) {
        refreshTokenService.findByToken(refreshToken)
            .ifPresent(token -> refreshTokenService.deleteByUser(token.getUser()));
    }
}
