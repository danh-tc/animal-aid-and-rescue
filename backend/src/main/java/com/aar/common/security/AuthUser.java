package com.aar.common.security;

import com.aar.system.Role;

/** Người dùng đã xác thực, lấy từ JWT, không cần truy vấn database ở mỗi request. */
public record AuthUser(Long id, String email, Role role) {
}
