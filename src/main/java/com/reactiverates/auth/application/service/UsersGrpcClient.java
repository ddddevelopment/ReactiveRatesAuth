package com.reactiverates.auth.application.service;

import com.reactiverates.auth.domain.model.UserDto;
import com.reactiverates.users.grpc.CreateUserRequest;
import com.reactiverates.users.grpc.GetUserByIdRequest;
import com.reactiverates.users.grpc.GetUserByUsernameRequest;
import com.reactiverates.users.grpc.UserResponse;
import com.reactiverates.users.grpc.UsersServiceGrpc;
import com.reactiverates.users.grpc.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersGrpcClient {
    
    private final UsersServiceGrpc.UsersServiceBlockingStub stub;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Создает нового пользователя через gRPC сервис
     */
    public UserResponse createUser(String username, String email, String password, 
                                 String firstName, String lastName, String phoneNumber) {
        try {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername(username)
                .setEmail(email)
                .setPassword(password)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPhoneNumber(phoneNumber)
                .setRole(UserRole.USER)
                .build();
            
            log.info("Creating user via gRPC: {}", username);
            UserResponse response = stub.createUser(request);
            log.info("User created successfully: {}", response.getUsername());
            
            return response;
        } catch (Exception e) {
            log.error("Error creating user via gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user via gRPC", e);
        }
    }
    
    /**
     * Получает пользователя по ID через gRPC сервис
     */
    public UserResponse getUserById(Long userId) {
        try {
            GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                .setUserId(userId)
                .build();
            
            log.info("Getting user by ID via gRPC: {}", userId);
            UserResponse response = stub.getUserById(request);
            log.info("User retrieved successfully: {}", response.getUsername());
            
            return response;
        } catch (Exception e) {
            log.error("Error getting user by ID via gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user by ID via gRPC", e);
        }
    }
    
    /**
     * Получает пользователя по username через gRPC сервис
     */
    public UserResponse getUserByUsername(String username) {
        try {
            GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                .setUsername(username)
                .build();
            
            log.info("Getting user by username via gRPC: {}", username);
            UserResponse response = stub.getUserByUsername(request);
            log.info("User retrieved successfully: {}", response.getUsername());
            
            return response;
        } catch (Exception e) {
            log.error("Error getting user by username via gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user by username via gRPC", e);
        }
    }
    
    /**
     * Загружает пользователя для Spring Security аутентификации
     * Этот метод используется CustomUserDetailsService
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UserResponse userResponse = getUserByUsername(username);
            
            // Создаем UserDto с password_hash из gRPC ответа
            UserDto userDto = new UserDto(userResponse);
            
            if (!userDto.isEnabled()) {
                throw new UsernameNotFoundException("User account is disabled: " + username);
            }
            
            return userDto;
        } catch (Exception e) {
            log.error("Error loading user by username via gRPC: {}", e.getMessage(), e);
            throw new UsernameNotFoundException("User not found: " + username, e);
        }
    }
    
    /**
     * Проверяет учетные данные пользователя
     * Этот метод используется для аутентификации
     */
    public boolean authenticateUser(String username, String rawPassword) {
        try {
            UserResponse userResponse = getUserByUsername(username);
            
            if (!userResponse.getIsActive()) {
                log.warn("User account is disabled: {}", username);
                return false;
            }
            
            // Проверяем пароль, используя password_hash из gRPC ответа
            String storedPasswordHash = userResponse.getPasswordHash();
            if (storedPasswordHash == null || storedPasswordHash.isEmpty()) {
                log.warn("Password hash is null or empty for user: {}", username);
                return false;
            }
            
            boolean passwordMatches = passwordEncoder.matches(rawPassword, storedPasswordHash);
            
            if (passwordMatches) {
                log.info("User authentication successful: {}", username);
            } else {
                log.warn("Password mismatch for user: {}", username);
            }
            
            return passwordMatches;
            
        } catch (Exception e) {
            log.error("Error authenticating user via gRPC: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получает UserDto по username для использования в AuthService
     */
    public UserDto getGrpcUserByUsername(String username) {
        try {
            UserResponse userResponse = getUserByUsername(username);
            return new UserDto(userResponse); // Используем новый конструктор с password_hash
        } catch (Exception e) {
            log.error("Error getting UserDto by username: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user via gRPC", e);
        }
    }
}
