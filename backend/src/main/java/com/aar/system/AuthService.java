package com.aar.system;

import com.aar.common.error.BusinessException;
import com.aar.common.security.AuthUser;
import com.aar.common.security.JwtService;
import com.aar.system.AuthDtos.AuthResponse;
import com.aar.system.AuthDtos.GoogleAuthResponse;
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
    private final GoogleTokenVerifier google;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       GoogleTokenVerifier google) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.google = google;
    }

    /**
     * Đăng nhập, đăng ký nhanh bằng Google (FR-04). Email đã có tài khoản thì đăng nhập vào tài khoản đó
     * (kể cả nhân viên), chưa có thì tạo tài khoản Người nhận nuôi. Mật khẩu ngẫu nhiên, người dùng vẫn
     * đăng nhập được bằng Google, muốn dùng mật khẩu thì nhờ Quản lý đặt lại.
     */
    @Transactional
    public GoogleAuthResponse loginWithGoogle(String credential) {
        GoogleTokenVerifier.GoogleIdentity id = google.verify(credential);
        if (id.email() == null || !id.emailVerified()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "GOOGLE_EMAIL_UNVERIFIED",
                "Email Google chưa được xác minh, vui lòng dùng email khác");
        }
        String email = id.email().toLowerCase();
        var existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isActive()) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa");
            }
            AuthResponse r = toResponse(user);
            return new GoogleAuthResponse(r.accessToken(), r.expiresInMinutes(), r.user(), false);
        }
        String name = id.name() == null || id.name().isBlank() ? email.substring(0, email.indexOf('@')) : id.name().trim();
        // 32 byte ngẫu nhiên (43 ký tự), dưới giới hạn 72 byte của BCrypt
        byte[] random = new byte[32];
        new java.security.SecureRandom().nextBytes(random);
        String randomPassword = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        User user = users.save(new User(email, passwordEncoder.encode(randomPassword),
            name.length() > 150 ? name.substring(0, 150) : name, null, Role.ADOPTER));
        AuthResponse r = toResponse(user);
        return new GoogleAuthResponse(r.accessToken(), r.expiresInMinutes(), r.user(), true);
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
