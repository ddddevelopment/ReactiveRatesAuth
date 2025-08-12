package com.reactiverates.auth.domain.model;

import com.reactiverates.users.grpc.UserResponse;
import com.reactiverates.users.grpc.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDto implements UserDetails {
    
    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final UserRole role;
    private final boolean isActive;
    private final String createdAt;
    private final String updatedAt;
    private final String fullName;

    public UserDto(UserResponse userResponse, String password) {
        this.id = userResponse.getId();
        this.username = userResponse.getUsername();
        this.email = userResponse.getEmail();
        this.password = password; // Пароль должен передаваться отдельно, так как его нет в UserResponse
        this.firstName = userResponse.getFirstName();
        this.lastName = userResponse.getLastName();
        this.phoneNumber = userResponse.getPhoneNumber();
        this.role = userResponse.getRole();
        this.isActive = userResponse.getIsActive();
        this.createdAt = userResponse.getCreatedAt();
        this.updatedAt = userResponse.getUpdatedAt();
        this.fullName = userResponse.getFullName();
    }

    /**
     * Конструктор для создания UserDto из UserResponse с password_hash
     */
    public UserDto(UserResponse userResponse) {
        this.id = userResponse.getId();
        this.username = userResponse.getUsername();
        this.email = userResponse.getEmail();
        this.password = userResponse.getPasswordHash(); // Используем password_hash из gRPC ответа
        this.firstName = userResponse.getFirstName();
        this.lastName = userResponse.getLastName();
        this.phoneNumber = userResponse.getPhoneNumber();
        this.role = userResponse.getRole();
        this.isActive = userResponse.getIsActive();
        this.createdAt = userResponse.getCreatedAt();
        this.updatedAt = userResponse.getUpdatedAt();
        this.fullName = userResponse.getFullName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = "ROLE_" + role.name();
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isActive;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // Геттеры для дополнительных полей
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getFullName() {
        return fullName;
    }
}
