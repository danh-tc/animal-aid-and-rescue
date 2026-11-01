package com.aar.handover;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** UC-11: bàn giao, hậu kiểm và trả lại (người C, báo cáo mục 2.1.5). */
class HandoverFlowIntegrationTest extends IntegrationTestBase {

    private static final Object PASS = criteria("PASS", null);

    @Test
    void handoverCreatesTwoFollowUpsAndAdoptsPet() throws Exception {
        long appId = approvedApplication(ADOPTER1);
        long petId = jdbc.queryForObject("SELECT pet_id FROM adoption_applications WHERE id = ?", Long.class, appId);

        JsonNode handover = postJson("/api/staff/applications/" + appId + "/handover", STAFF,
            map("note", "Ký biên bản"), 201);
        assertThat(handover.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(handover.get("followUps")).hasSize(2);
        assertThat(handover.get("followUps").get(0).get("dueDate").asText())
            .isEqualTo(LocalDate.now().plusDays(14).toString());
        assertThat(handover.get("followUps").get(1).get("dueDate").asText())
            .isEqualTo(LocalDate.now().plusDays(60).toString());
        assertThat(petStatus(petId)).isEqualTo("ADOPTED");
        assertThat(applicationStatus(appId)).isEqualTo("HANDED_OVER");

        // RB-06: một đơn tối đa một bàn giao
        postJson("/api/staff/applications/" + appId + "/handover", STAFF, null, 409);

        JsonNode adoptions = getJson("/api/adopter/adoptions", ADOPTER1, 200);
        assertThat(adoptions.findValuesAsText("petId")).contains(Long.toString(petId));
        getJson("/api/adopter/adoptions", STAFF, 403);
    }

    /** QĐ-12: Cần theo dõi thì thêm một lần bổ sung, đạt hết thì kết thúc hậu kiểm. */
    @Test
    void watchAddsExtraFollowUpThenAllPassCompletes() throws Exception {
        JsonNode handover = handedOver(ADOPTER2);
        long first = handover.get("followUps").get(0).get("id").asLong();
        long second = handover.get("followUps").get(1).get("id").asLong();

        JsonNode noReason = postJson("/api/staff/follow-ups/" + first + "/result", STAFF, criteria("WATCH", null), 400);
        assertThat(noReason.get("code").asText()).isEqualTo("REASON_REQUIRED");

        JsonNode watch = postJson("/api/staff/follow-ups/" + first + "/result", STAFF,
            criteria("WATCH", "Chưa thích nghi"), 200);
        assertThat(watch.get("handoverStatus").asText()).isEqualTo("IN_PROGRESS");
        JsonNode extra = watch.get("additionalFollowUp");
        assertThat(extra.get("sequenceNo").asInt()).isEqualTo(3);
        assertThat(extra.get("additional").asBoolean()).isTrue();

        // Lần đã ghi không ghi lại
        postJson("/api/staff/follow-ups/" + first + "/result", STAFF, PASS, 409);

        assertThat(postJson("/api/staff/follow-ups/" + extra.get("id").asLong() + "/result", STAFF, PASS, 200)
            .get("handoverStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(postJson("/api/staff/follow-ups/" + second + "/result", STAFF, PASS, 200)
            .get("handoverStatus").asText()).isEqualTo("COMPLETED");
    }

    /** QĐ-12: Không đạt => chờ thu hồi, hủy lịch còn lại. Khi thú về thì tạo lượt tiếp nhận RETURN. */
    @Test
    void failedFollowUpLeadsToRecallAndReturn() throws Exception {
        JsonNode handover = handedOver(ADOPTER3);
        long petId = handover.get("petId").asLong();
        long first = handover.get("followUps").get(0).get("id").asLong();

        JsonNode fail = postJson("/api/staff/follow-ups/" + first + "/result", STAFF,
            criteria("FAIL", "Chủ tự ý cho thú"), 200);
        assertThat(fail.get("handoverStatus").asText()).isEqualTo("RECALL_PENDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM follow_ups WHERE handover_id = ? AND status = 'CANCELLED'",
            Integer.class, handover.get("id").asLong())).isEqualTo(1);

        JsonNode returned = postJson("/api/staff/pets/" + petId + "/returns", STAFF,
            map("reason", "Thu hồi sau hậu kiểm không đạt", "priority", "NEED_CHECK"), 201);
        assertThat(returned.get("petStatus").asText()).isEqualTo("IN_CARE");

        JsonNode pet = getJson("/api/staff/pets/" + petId, STAFF, 200);
        assertThat(pet.get("area").asText()).isEqualTo("QUARANTINE");
        assertThat(pet.get("intakes").get(0).get("source").asText()).isEqualTo("RETURN");
        assertThat(pet.get("intakes")).hasSize(2);
        JsonNode handovers = getJson("/api/staff/pets/" + petId + "/handovers", STAFF, 200);
        assertThat(handovers.get(0).get("status").asText()).isEqualTo("RETURNED");
    }

    /** QĐ-13: trả lại được chấp nhận cả sau khi hậu kiểm đã kết thúc. Thú chưa nhận nuôi thì không trả lại được. */
    @Test
    void returnAcceptedAfterFollowUpsCompleted() throws Exception {
        JsonNode handover = handedOver(ADOPTER1);
        long petId = handover.get("petId").asLong();
        for (JsonNode f : handover.get("followUps")) {
            postJson("/api/staff/follow-ups/" + f.get("id").asLong() + "/result", STAFF, PASS, 200);
        }
        postJson("/api/staff/pets/" + petId + "/returns", STAFF, map("reason", "", "priority", "STABLE"), 400);
        postJson("/api/staff/pets/" + petId + "/returns", STAFF,
            map("reason", "Chủ đi nước ngoài", "priority", "STABLE"), 201);
        JsonNode again = postJson("/api/staff/pets/" + petId + "/returns", STAFF,
            map("reason", "Lặp lại", "priority", "STABLE"), 409);
        assertThat(again.get("code").asText()).isEqualTo("PET_NOT_ADOPTED");
    }

    @Test
    void scheduledFollowUpsListedWithOwnerContact() throws Exception {
        JsonNode handover = handedOver(ADOPTER2);
        long firstId = handover.get("followUps").get(0).get("id").asLong();
        JsonNode due = getJson("/api/staff/follow-ups?dueBefore=" + LocalDate.now().plusDays(20), STAFF, 200);
        JsonNode mine = null;
        for (JsonNode item : due) {
            if (item.get("followUp").get("id").asLong() == firstId) {
                mine = item;
            }
        }
        assertThat(mine).isNotNull();
        assertThat(mine.get("ownerName").asText()).isEqualTo("Võ Quốc Bảo");
        // Lần 2 (sau 60 ngày) chưa đến hạn trong 20 ngày tới
        assertThat(due.findValuesAsText("id")).doesNotContain(handover.get("followUps").get(1).get("id").asText());
    }

    private long approvedApplication(String adopter) throws Exception {
        long petId = availablePet(false);
        long appId = submitApplication(petId, adopter, false);
        postJson("/api/staff/applications/" + appId + "/approve", STAFF, null, 200);
        return appId;
    }

    private JsonNode handedOver(String adopter) throws Exception {
        return postJson("/api/staff/applications/" + approvedApplication(adopter) + "/handover", STAFF, null, 201);
    }

    private static Object criteria(String result, String reason) {
        boolean ok = "PASS".equals(result);
        return map("result", result, "healthOk", true, "housingOk", ok, "commitmentOk", true,
            "petPresent", true, "ownerReachable", true, "reason", reason);
    }
}
