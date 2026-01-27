package com.legalconnect.lawyerbooking.security;

import java.security.Principal;

public class UserPrincipal implements Principal {
    private final Long userId;
    private final String username;
    private final String role;

    public UserPrincipal(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
