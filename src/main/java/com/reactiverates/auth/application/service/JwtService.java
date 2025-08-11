package com.reactiverates.auth.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.reactiverates.auth.infrastructure.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public String generateAccessToken(UserDetails userDetails) {
        return jwtUtil.generateAccessToken(userDetails);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return jwtUtil.generateRefreshToken(userDetails);
    }
    
    public String generateRefreshToken(UserDetails userDetails, String tokenId) {
        return jwtUtil.generateRefreshToken(userDetails, tokenId);
    }

    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }
    
    public String extractTokenId(String token) {
        return jwtUtil.extractTokenId(token);
    }
    
    public List<Map<String, String>> extractAuthorities(String token) {
        return jwtUtil.extractAuthorities(token);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return jwtUtil.isTokenValid(token, userDetails);
    }

    public boolean isAccessToken(String token) {
        return jwtUtil.isAccessToken(token);
    }

    public boolean isRefreshToken(String token) {
        return jwtUtil.isRefreshToken(token);
    }

    public void setAuthentication(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}