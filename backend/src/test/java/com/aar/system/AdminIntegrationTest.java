package com.aar.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class AdminIntegrationTest extends IntegrationTestBase {

    @Test
    void managerCreatesAndLocksStaffAccount() throws Exception {
        JsonNode created = postJson("/api/admin/users", MANAGER, map("email", "nv.moi@aar.local",
            "password", "Password@123", "fullName", "Nhân viên mới", "role", "STAFF"), 201);
        long id = created.get("id").asLong();
        assertThat(created.get("active").asBoolean()).isTrue();
        token("nv.moi@aar.local");

        postJson("/api/admin/users", MANAGER, map("email", "nv.moi@aar.local", "password", "Password@123",
            "fullName", "Trùng", "role", "STAFF"), 409);
        postJson("/api/admin/users", MANAGER, map("email", "kh@aar.local", "password", "Password@123",
            "fullName", "Khách", "role", "ADOPTER"), 400);

        patchJson("/api/admin/users/" + id, MANAGER, map("active", false), 200);
        JsonNode login = postJson("/api/public/auth/login", null,
            map("email", "nv.moi@aar.local", "password", "Password@123"), 403);
        assertThat(login.get("code").asText()).isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    void managerCannotLockThemself() throws Exception {
        JsonNode error = patchJson("/api/admin/users/1", MANAGER, map("active", false), 400);
        assertThat(error.get("code").asText()).isEqualTo("SELF_LOCKOUT");
    }

    @Test
    void staffCannotUseAdminApis() throws Exception {
        getJson("/api/admin/users", STAFF, 403);
        getJson("/api/admin/system-parameters", STAFF, 403);
        assertThat(getJson("/api/staff/vet-clinics", STAFF, 200).size()).isGreaterThanOrEqualTo(2);
        assertThat(getJson("/api/staff/staff-members", STAFF, 200).findValuesAsText("role"))
            .doesNotContain("ADOPTER");
    }

    @Test
    void clinicCatalogue() throws Exception {
        JsonNode clinic = postJson("/api/admin/vet-clinics", MANAGER, map("name", "Phòng khám Sài Gòn",
            "phone", "0280000009"), 201);
        long id = clinic.get("id").asLong();
        JsonNode updated = patchJson("/api/admin/vet-clinics/" + id, MANAGER,
            map("name", "Phòng khám Sài Gòn", "active", false), 200);
        assertThat(updated.get("active").asBoolean()).isFalse();
        assertThat(getJson("/api/staff/vet-clinics", STAFF, 200).findValuesAsText("name"))
            .doesNotContain("Phòng khám Sài Gòn");
    }

    @Test
    void systemParametersValidated() throws Exception {
        JsonNode list = getJson("/api/admin/system-parameters", MANAGER, 200);
        assertThat(list.findValuesAsText("key")).contains("QUARANTINE_DAYS", "MIN_STERILIZATION_AGE_WEEKS");
        putJson("/api/admin/system-parameters/APPLICATION_ALERT_DAYS", MANAGER, map("value", "abc"), 400);
        putJson("/api/admin/system-parameters/APPLICATION_ALERT_DAYS", MANAGER, map("value", "0"), 400);
        putJson("/api/admin/system-parameters/KHONG_CO", MANAGER, map("value", "3"), 404);
        assertThat(putJson("/api/admin/system-parameters/APPLICATION_ALERT_DAYS", MANAGER, map("value", "10"), 200)
            .get("value").asText()).isEqualTo("10");
    }
}
