package com.aar.system;

import com.aar.common.error.BusinessException;
import com.aar.common.security.AuthUser;
import com.aar.common.security.JwtService;
import com.aar.system.AuthDtos.AuthResponse;
import com.aar.system.AuthDtos.LoginRequest;
import com.aar.system.AuthDtos.RegisterRequest;
import com.aar.system.AuthDtos.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Đăng ký công khai luôn tạo tài khoản Người nhận nuôi. Tài khoản nhân viên do Quản lý tạo. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw BusinessException.conflict("EMAIL_TAKEN", "Email đã được sử dụng");
        }
        User user = users.save(new User(req.email().toLowerCase(), passwordEncoder.encode(req.password()),
            req.fullName(), req.phone(), Role.ADOPTER));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email())
            .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                "Email hoặc mật khẩu không đúng"));
        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.issue(new AuthUser(user.getId(), user.getEmail(), user.getRole()));
        return new AuthResponse(token, jwtService.expirationMinutes(), UserView.of(user));
    }
}
