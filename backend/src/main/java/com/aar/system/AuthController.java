package com.aar.system;

import com.aar.common.config.AppProperties;
import com.aar.system.AuthDtos.AuthConfig;
import com.aar.system.AuthDtos.AuthResponse;
import com.aar.system.AuthDtos.GoogleAuthResponse;
import com.aar.system.AuthDtos.GoogleLoginRequest;
import com.aar.system.AuthDtos.LoginRequest;
import com.aar.system.AuthDtos.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Màn hình P05: đăng ký, đăng nhập bằng mật khẩu hoặc bằng Google. */
@RestController
@RequestMapping("/api/public/auth")
public class AuthController {

    private final AuthService authService;
    private final AppProperties props;

    public AuthController(AuthService authService, AppProperties props) {
        this.authService = authService;
        this.props = props;
    }

    /** Cấu hình công khai cho trang đăng nhập: có bật Google hay không và Client ID. */
    @GetMapping("/config")
    public AuthConfig config() {
        boolean enabled = props.google() != null && props.google().enabled();
        return new AuthConfig(enabled, enabled ? props.google().clientId() : null);
    }

    /** Đăng nhập hoặc đăng ký nhanh bằng Google (FR-04). */
    @PostMapping("/google")
    public GoogleAuthResponse google(@Valid @RequestBody GoogleLoginRequest req) {
        return authService.loginWithGoogle(req.credential());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
