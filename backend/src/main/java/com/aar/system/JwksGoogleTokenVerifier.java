package com.aar.system;

import com.aar.common.config.AppProperties;
import com.aar.common.error.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra chữ ký RS256 bằng khóa công khai của Google (JWKS, có cache), hạn dùng,
 * nơi phát hành (accounts.google.com) và audience phải đúng Client ID của hệ thống.
 */
@Component
public class JwksGoogleTokenVerifier implements GoogleTokenVerifier {

    private static final String JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> ISSUERS = List.of("accounts.google.com", "https://accounts.google.com");

    private final AppProperties.Google config;
    private volatile NimbusJwtDecoder decoder;

    public JwksGoogleTokenVerifier(AppProperties props) {
        this.config = props.google();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (config == null || !config.enabled()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_DISABLED",
                "Đăng nhập bằng Google chưa được cấu hình");
        }
        Jwt jwt;
        try {
            jwt = decoder().decode(idToken);
        } catch (JwtException e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID",
                "Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn, vui lòng thử lại");
        }
        Object verified = jwt.getClaims().get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equals(String.valueOf(verified));
        return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"), emailVerified,
            jwt.getClaimAsString("name"));
    }

    private NimbusJwtDecoder decoder() {
        NimbusJwtDecoder d = decoder;
        if (d == null) {
            d = NimbusJwtDecoder.withJwkSetUri(JWKS).build();
            OAuth2TokenValidator<Jwt> issuer = jwt -> ISSUERS.contains(String.valueOf(jwt.getClaims().get("iss")))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sai nơi phát hành", null));
            OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(config.clientId())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sai audience", null));
            d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience));
            decoder = d;
        }
        return d;
    }
}
