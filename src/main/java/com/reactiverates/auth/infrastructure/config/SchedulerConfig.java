package com.reactiverates.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.reactiverates.auth.application.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {
    
    private final RefreshTokenService refreshTokenService;
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredTokens() {
        refreshTokenService.deleteExpiredTokens();
    }
} 