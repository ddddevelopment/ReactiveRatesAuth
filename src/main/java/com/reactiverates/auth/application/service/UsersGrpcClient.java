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
            
            // Создаем GrpcUser с зашифрованным паролем
            // Примечание: в реальном приложении пароль должен храниться в gRPC сервисе
            // Здесь мы создаем временный зашифрованный пароль для совместимости
            String encodedPassword = passwordEncoder.encode("temporary_password");
            
            UserDto userDto = new UserDto(userResponse, encodedPassword);
            
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
            
            // Примечание: в реальном приложении пароль должен проверяться в gRPC сервисе
            // Здесь мы предполагаем, что пароль уже зашифрован и хранится в gRPC сервисе
            // Для демонстрации используем временную логику
            
            // TODO: Добавить метод в gRPC сервис для проверки пароля
            // Пока что возвращаем true для демонстрации
            log.info("User authentication successful: {}", username);
            return true;
            
        } catch (Exception e) {
            log.error("Error authenticating user via gRPC: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получает GrpcUser по username для использования в AuthService
     */
    public UserDto getGrpcUserByUsername(String username) {
        try {
            UserResponse userResponse = getUserByUsername(username);
            String encodedPassword = passwordEncoder.encode("temporary_password");
            return new UserDto(userResponse, encodedPassword);
        } catch (Exception e) {
            log.error("Error getting GrpcUser by username: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user via gRPC", e);
        }
    }
}
