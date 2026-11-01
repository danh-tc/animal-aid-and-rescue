package com.aar.system;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu dài từ 8 đến 72 ký tự") String password,
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "^[0-9+]{9,15}$", message = "Số điện thoại không hợp lệ") String phone) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserView(Long id, String email, String fullName, Role role) {

        static UserView of(User user) {
            return new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        }
    }

    public record AuthResponse(String accessToken, long expiresInMinutes, UserView user) {
    }
}
