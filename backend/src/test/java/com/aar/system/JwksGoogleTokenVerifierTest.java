package com.aar.system;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aar.common.config.AppProperties;
import com.aar.common.error.BusinessException;
import org.junit.jupiter.api.Test;

/** Không cần mạng: chưa cấu hình Client ID thì báo tắt, token rác thì báo không hợp lệ trước khi gọi Google. */
class JwksGoogleTokenVerifierTest {

    @Test
    void disabledWithoutClientId() {
        var verifier = new JwksGoogleTokenVerifier(new AppProperties(null, null, null, null, new AppProperties.Google("")));
        assertThatThrownBy(() -> verifier.verify("abc"))
            .isInstanceOf(BusinessException.class).hasMessageContaining("chưa được cấu hình");
    }

    @Test
    void malformedTokenIsRejected() {
        var verifier = new JwksGoogleTokenVerifier(
            new AppProperties(null, null, null, null, new AppProperties.Google("123.apps.googleusercontent.com")));
        assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
            .isInstanceOf(BusinessException.class).hasMessageContaining("không hợp lệ");
    }
}
