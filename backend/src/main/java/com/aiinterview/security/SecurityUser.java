package com.aiinterview.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SecurityUser {

    private Long userId;
    private String username;
    private String role;
}
