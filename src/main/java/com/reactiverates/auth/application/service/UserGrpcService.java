package com.reactiverates.auth.application.service;

import com.reactiverates.users.grpc.CreateUserRequest;
import com.reactiverates.users.grpc.GetUserByIdRequest;
import com.reactiverates.users.grpc.GetUserByUsernameRequest;
import com.reactiverates.users.grpc.UserResponse;
import com.reactiverates.users.grpc.UsersServiceGrpc;
import com.reactiverates.users.grpc.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService {
    
    private final UsersServiceGrpc.UsersServiceBlockingStub stub;
    
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
}
