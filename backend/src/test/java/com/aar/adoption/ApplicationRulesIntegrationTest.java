package com.aar.adoption;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Quy tắc bổ sung sau rà soát báo cáo: liên hệ bắt buộc, tối đa đơn chờ, ngày hẹn bàn giao, cảnh báo người nộp. */
class ApplicationRulesIntegrationTest extends IntegrationTestBase {

    private static Object application(long petId, String phone, String address) {
        return map("petId", petId, "housingType", "HOUSE", "hasPetExperience", true, "phone", phone, "address", address);
    }

    @Test
    void contactRequiredWhenAccountHasNone() throws Exception {
        String adopter = newAdopter(null);
        long petId = availablePet(false);
        JsonNode missing = postJson("/api/adopter/applications", adopter, application(petId, null, null), 400);
        assertThat(missing.get("code").asText()).isEqualTo("CONTACT_REQUIRED");
        postJson("/api/adopter/applications", adopter, application(petId, "0909123456", " "), 400);
        postJson("/api/adopter/applications", adopter, application(petId, "abc", "1 Lê Lợi"), 400);

        JsonNode ok = postJson("/api/adopter/applications", adopter, application(petId, "0909123456", "1 Lê Lợi, Quận 1"), 201);
        assertThat(ok.get("applicant").get("phone").asText()).isEqualTo("0909123456");
        JsonNode me = getJson("/api/me", adopter, 200);
        assertThat(me.get("phone").asText()).isEqualTo("0909123456");
        assertThat(me.get("address").asText()).isEqualTo("1 Lê Lợi, Quận 1");

        // Lần sau tài khoản đã có, không phải nhập lại
        postJson("/api/adopter/applications", adopter, application(availablePet(false), null, null), 201);
    }

    @Test
    void atMostFivePendingApplications() throws Exception {
        jdbc.update("UPDATE system_parameters SET param_value = '2' WHERE param_key = 'MAX_PENDING_APPLICATIONS'");
        String adopter = newAdopter("0909000111");
        long first = postJson("/api/adopter/applications", adopter, application(availablePet(false), null, "Quận 3"), 201)
            .get("id").asLong();
        postJson("/api/adopter/applications", adopter, application(availablePet(false), null, null), 201);
        JsonNode third = postJson("/api/adopter/applications", adopter, application(availablePet(false), null, null), 409);
        assertThat(third.get("code").asText()).isEqualTo("TOO_MANY_PENDING");

        postJson("/api/adopter/applications/" + first + "/cancel", adopter, map("reason", "Đổi ý"), 200);
        postJson("/api/adopter/applications", adopter, application(availablePet(false), null, null), 201);

        jdbc.update("UPDATE system_parameters SET param_value = '5' WHERE param_key = 'MAX_PENDING_APPLICATIONS'");
        assertThat(getJson("/api/admin/system-parameters", MANAGER, 200).findValuesAsText("key"))
            .contains("MAX_PENDING_APPLICATIONS");
    }

    @Test
    void handoverDateSetOnApprovalAndChangeable() throws Exception {
        long petId = availablePet(false);
        long appId = submitApplication(petId, ADOPTER1);
        String in3days = LocalDate.now().plusDays(3).toString();

        postJson("/api/staff/applications/" + appId + "/handover-date", STAFF, map("handoverDate", in3days), 409);
        postJson("/api/staff/applications/" + appId + "/approve", STAFF,
            map("handoverDate", LocalDate.now().minusDays(1).toString()), 400);
        JsonNode approved = postJson("/api/staff/applications/" + appId + "/approve", STAFF,
            map("note", "Hẹn đón bé", "handoverDate", in3days), 200);
        assertThat(approved.get("handoverDate").asText()).isEqualTo(in3days);

        String in5days = LocalDate.now().plusDays(5).toString();
        assertThat(postJson("/api/staff/applications/" + appId + "/handover-date", STAFF, map("handoverDate", in5days), 200)
            .get("handoverDate").asText()).isEqualTo(in5days);
        JsonNode mine = getJson("/api/adopter/applications", ADOPTER1, 200);
        for (JsonNode a : mine) {
            if (a.get("id").asLong() == appId) {
                assertThat(a.get("handoverDate").asText()).isEqualTo(in5days);
            }
        }
        postJson("/api/staff/applications/" + appId + "/handover-date", ADOPTER1, map("handoverDate", in5days), 403);
    }

    @Test
    void reviewerSeesApplicantRecallAndReturnHistory() throws Exception {
        // Người A bị thu hồi: hậu kiểm Không đạt rồi thú về trung tâm
        String recalled = newAdopter("0909000222");
        long pet1 = availablePet(false);
        long app1 = postJson("/api/adopter/applications", recalled, application(pet1, null, "Quận 5"), 201).get("id").asLong();
        postJson("/api/staff/applications/" + app1 + "/approve", STAFF, null, 200);
        JsonNode handover = postJson("/api/staff/applications/" + app1 + "/handover", STAFF, null, 201);
        postJson("/api/staff/follow-ups/" + handover.get("followUps").get(0).get("id").asLong() + "/result", STAFF,
            map("result", "FAIL", "contactMethod", "HOME_VISIT", "healthOk", false, "housingOk", false,
                "commitmentOk", false, "petPresent", true, "ownerReachable", true, "reason", "Bỏ đói thú"), 200);
        postJson("/api/staff/pets/" + pet1 + "/returns", STAFF, map("reason", "Thu hồi", "priority", "URGENT"), 201);

        long app2 = postJson("/api/adopter/applications", recalled, application(availablePet(false), null, null), 201)
            .get("id").asLong();
        JsonNode history = getJson("/api/staff/applications/" + app2, STAFF, 200).get("applicantHistory");
        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("kind").asText()).isEqualTo("RECALLED");
        assertThat(history.get(0).get("note").asText()).contains("thu hồi");

        // Người nhận nuôi trong dữ liệu mẫu (Phạm Thu Hà) từng tự trả lại Lu
        long app3 = submitApplication(availablePet(false), ADOPTER1);
        JsonNode seedHistory = getJson("/api/staff/applications/" + app3, STAFF, 200).get("applicantHistory");
        assertThat(seedHistory.findValuesAsText("kind")).contains("RETURNED");

        // Người chưa từng nhận nuôi không có cảnh báo
        String fresh = newAdopter("0909000333");
        long app4 = postJson("/api/adopter/applications", fresh, application(availablePet(false), null, "Quận 7"), 201)
            .get("id").asLong();
        assertThat(getJson("/api/staff/applications/" + app4, STAFF, 200).get("applicantHistory")).isEmpty();
    }
}
