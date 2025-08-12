package com.reactiverates.auth.domain.service;

import com.reactiverates.auth.domain.model.AuthResponse;
import com.reactiverates.auth.domain.model.LoginRequest;
import com.reactiverates.auth.domain.model.LogoutResponse;
import com.reactiverates.auth.domain.model.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshTokenJwt);

    LogoutResponse logout(String refreshTokenJwt);

}