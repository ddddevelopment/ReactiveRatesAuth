package com.reactiverates.auth.application.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.reactiverates.auth.infrastructure.grpc.UsersGrpcClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UsersGrpcClient usersGrpcClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.info("Loading user details for username: {}", username);
            UserDetails userDetails = usersGrpcClient.loadUserByUsername(username);
            log.info("User details loaded successfully for username: {}", username);
            return userDetails;
        } catch (UsernameNotFoundException e) {
            log.warn("User not found: {}", username);
            throw e;
        } catch (Exception e) {
            log.error("Error loading user details for username: {}", username, e);
            throw new UsernameNotFoundException("Error loading user: " + username, e);
        }
    }
}