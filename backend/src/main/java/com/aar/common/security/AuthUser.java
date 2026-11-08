package com.aar.common.security;

import com.aar.system.Role;

public record AuthUser(Long id, String email, Role role) {
}
