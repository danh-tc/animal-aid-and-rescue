package com.aar.system;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record UserAdminView(Long id, String email, String fullName, String phone, String address, Role role,
                                boolean active, OffsetDateTime createdAt) {

        static UserAdminView of(User u) {
            return new UserAdminView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getAddress(),
                u.getRole(), u.isActive(), u.getCreatedAt());
        }
    }

    public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu dài từ 8 đến 72 ký tự") String password,
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "^[0-9+]{9,15}$", message = "Số điện thoại không hợp lệ") String phone,
        @NotNull Role role) {
    }

    /** Trường null nghĩa là giữ nguyên. */
    public record UpdateUserRequest(
        @Size(max = 150) String fullName,
        @Pattern(regexp = "^[0-9+]{9,15}$", message = "Số điện thoại không hợp lệ") String phone,
        @Size(max = 255) String address,
        Role role,
        Boolean active,
        @Size(min = 8, max = 72, message = "Mật khẩu dài từ 8 đến 72 ký tự") String password) {
    }

    public record ClinicView(Long id, String name, String address, String phone, boolean active) {

        public static ClinicView of(VetClinic c) {
            return new ClinicView(c.getId(), c.getName(), c.getAddress(), c.getPhone(), c.isActive());
        }
    }

    public record ClinicRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 255) String address,
        @Size(max = 20) String phone,
        Boolean active) {
    }

    public record ParameterView(String key, String value, String description) {

        static ParameterView of(SystemParameter p) {
            return new ParameterView(p.getKey(), p.getValue(), p.getDescription());
        }
    }

    public record ParameterRequest(@NotBlank String value) {
    }

    public record StaffMember(Long id, String fullName, Role role) {
    }
}
