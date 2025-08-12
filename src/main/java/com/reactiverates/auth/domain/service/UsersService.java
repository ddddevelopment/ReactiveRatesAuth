package com.reactiverates.auth.domain.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.reactiverates.auth.domain.model.UserDto;

public interface UsersService {

    UserDto createUser(String username, String email, String password,
            String firstName, String lastName, String phoneNumber);

    Optional<UserDto> getUserById(Long userId);

    Optional<UserDto> getUserByUsername(String username);

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    boolean authenticateUser(String username, String rawPassword);
}