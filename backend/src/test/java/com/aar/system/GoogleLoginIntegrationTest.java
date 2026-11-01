package com.aar.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aar.common.error.BusinessException;
import com.aar.support.IntegrationTestBase;
import com.aar.system.GoogleTokenVerifier.GoogleIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** FR-04: đăng nhập, đăng ký nhanh bằng Google. Chữ ký token do JwksGoogleTokenVerifier kiểm, ở đây thay bằng bản giả. */
class GoogleLoginIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    GoogleTokenVerifier verifier;

    @Test
    void firstGoogleLoginCreatesAdopterThenLogsIn() throws Exception {
        String email = "google-" + UUID.randomUUID() + "@gmail.com";
        when(verifier.verify(eq("tok-new"))).thenReturn(new GoogleIdentity("sub-1", email.toUpperCase(), true, "Nguyễn Google"));

        JsonNode first = postJson("/api/public/auth/google", null, map("credential", "tok-new"), 200);
        assertThat(first.get("newAccount").asBoolean()).isTrue();
        assertThat(first.get("user").get("role").asText()).isEqualTo("ADOPTER");
        assertThat(first.get("user").get("email").asText()).isEqualTo(email);
        assertThat(first.get("user").get("fullName").asText()).isEqualTo("Nguyễn Google");
        assertThat(first.get("accessToken").asText()).isNotBlank();

        JsonNode again = postJson("/api/public/auth/google", null, map("credential", "tok-new"), 200);
        assertThat(again.get("newAccount").asBoolean()).isFalse();
        assertThat(again.get("user").get("id").asLong()).isEqualTo(first.get("user").get("id").asLong());

        // Token hệ thống dùng được như đăng nhập mật khẩu
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/adopter/applications")
                .header("Authorization", "Bearer " + again.get("accessToken").asText()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    void existingStaffEmailLogsIntoStaffAccount() throws Exception {
        when(verifier.verify(eq("tok-staff"))).thenReturn(new GoogleIdentity("sub-2", STAFF, true, "Ai đó"));
        JsonNode res = postJson("/api/public/auth/google", null, map("credential", "tok-staff"), 200);
        assertThat(res.get("newAccount").asBoolean()).isFalse();
        assertThat(res.get("user").get("role").asText()).isEqualTo("STAFF");
    }

    @Test
    void rejectsUnverifiedEmailLockedAccountAndBadToken() throws Exception {
        when(verifier.verify(eq("tok-unverified")))
            .thenReturn(new GoogleIdentity("sub-3", "x-" + UUID.randomUUID() + "@gmail.com", false, "X"));
        assertThat(postJson("/api/public/auth/google", null, map("credential", "tok-unverified"), 401)
            .get("code").asText()).isEqualTo("GOOGLE_EMAIL_UNVERIFIED");

        String locked = "locked-" + UUID.randomUUID() + "@gmail.com";
        long id = postJson("/api/admin/users", MANAGER, map("email", locked, "password", "Password@123",
            "fullName", "Bị khóa", "role", "STAFF"), 201).get("id").asLong();
        patchJson("/api/admin/users/" + id, MANAGER, map("active", false), 200);
        when(verifier.verify(eq("tok-locked"))).thenReturn(new GoogleIdentity("sub-4", locked, true, "Bị khóa"));
        assertThat(postJson("/api/public/auth/google", null, map("credential", "tok-locked"), 403)
            .get("code").asText()).isEqualTo("ACCOUNT_LOCKED");

        when(verifier.verify(eq("tok-bad"))).thenThrow(
            new BusinessException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "Không hợp lệ"));
        postJson("/api/public/auth/google", null, map("credential", "tok-bad"), 401);
        postJson("/api/public/auth/google", null, map("credential", ""), 400);
    }

    @Test
    void configHidesGoogleWhenClientIdMissing() throws Exception {
        when(verifier.verify(anyString())).thenReturn(null);
        JsonNode config = getJson("/api/public/auth/config", null, 200);
        assertThat(config.get("googleEnabled").asBoolean()).isFalse();
        assertThat(config.get("googleClientId").isNull()).isTrue();
    }
}
