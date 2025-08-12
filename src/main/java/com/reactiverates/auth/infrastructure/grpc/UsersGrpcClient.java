package com.reactiverates.auth.infrastructure.grpc;

import com.reactiverates.auth.domain.model.UserDto;
import com.reactiverates.auth.domain.service.UsersService;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersGrpcClient implements UsersService {
    
    private final UsersServiceGrpc.UsersServiceBlockingStub stub;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public UserDto createUser(String username, String email, String password, 
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
            
            return new UserDto(response);
        } catch (Exception e) {
            log.error("Error creating user via gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user via gRPC", e);
        }
    }
    
    @Override
    public Optional<UserDto> getUserById(Long userId) {
        try {
            GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                .setUserId(userId)
                .build();
            
            log.info("Getting user by ID via gRPC: {}", userId);
            UserResponse response = stub.getUserById(request);
            
            if (response.getFound() == false) {
                log.warn("User not found: {}", userId);
                return Optional.empty();
            }
            
            log.info("User retrieved successfully: {}", response.getUsername());
            return Optional.of(new UserDto(response));
        } catch (Exception e) {
            log.error("Error getting user by ID via gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user by ID via gRPC", e);
        }
    }
    
    @Override
    public Optional<UserDto> getUserByUsername(String username) {
        try {
            GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                .setUsername(username)
                .build();
            
            log.info("Getting user by username via gRPC: {}", username);
            UserResponse response = stub.getUserByUsername(request);
            
            if (response.getFound() == false) {
                log.warn("User not found: {}", username);
                return Optional.empty();
            }
            
            log.info("User retrieved successfully: {}", response.getUsername());
            return Optional.of(new UserDto(response));
        } catch (Exception e) {
            log.error("Error getting user by username via gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user by username via gRPC", e);
        }
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Optional<UserDto> userDtoOptional = getUserByUsername(username);
            
            if (userDtoOptional.isEmpty()) {
                log.warn("User not found during authentication: {}", username);
                throw new UsernameNotFoundException("User not found: " + username);
            }
            
            UserDto userDto = userDtoOptional.get();
            
            if (!userDto.isActive()) {
                throw new UsernameNotFoundException("User account is disabled: " + username);
            }
            
            return userDto;
        } catch (Exception e) {
            log.error("Error loading user by username via gRPC: {}", e.getMessage(), e);
            throw new UsernameNotFoundException("Error loading user: " + username, e);
        }
    }
    
    @Override
    public boolean authenticateUser(String username, String rawPassword) {
        try {
            Optional<UserDto> userDtoOptional = getUserByUsername(username);
            
            if (userDtoOptional.isEmpty()) {
                log.warn("User not found during authentication: {}", username);
                return false;
            }
            
            UserDto userDto = userDtoOptional.get();
            
            if (!userDto.isActive()) {
                log.warn("User account is disabled: {}", username);
                return false;
            }
            
            String storedPasswordHash = userDto.getPassword();
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
}
