package com.aar.handover;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Đợt 2: sự kiện kết thúc vòng đời (mục 2.1.6, QĐ-15, QĐ-16) và tìm lại thú thất lạc. */
class EndEventIntegrationTest extends IntegrationTestBase {

    @Test
    void deceasedClosesOpenApplications() throws Exception {
        long petId = availablePet(false);
        long pending = submitApplication(petId, ADOPTER1, false);
        long pending2 = submitApplication(petId, ADOPTER2, false);
        postJson("/api/staff/applications/" + pending + "/approve", STAFF, null, 200);

        JsonNode res = postJson("/api/staff/pets/" + petId + "/end-events", STAFF,
            map("eventType", "DECEASED", "detail", "Suy tim đột ngột"), 201);
        assertThat(res.get("petStatus").asText()).isEqualTo("DECEASED");
        assertThat(res.get("cancelledApplications").asInt()).isEqualTo(1);
        assertThat(petStatus(petId)).isEqualTo("DECEASED");
        assertThat(applicationStatus(pending)).isEqualTo("CANCELLED");
        // Đơn thứ hai đã bị hệ thống tự đóng lúc duyệt, giữ nguyên
        assertThat(applicationStatus(pending2)).isEqualTo("CANCELLED");

        JsonNode events = getJson("/api/staff/pets/" + petId + "/end-events", STAFF, 200);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("eventType").asText()).isEqualTo("DECEASED");
        // Trạng thái kết thúc không ghi thêm sự kiện
        postJson("/api/staff/pets/" + petId + "/end-events", STAFF, map("eventType", "LOST", "detail", "x"), 409);
        // Hồ sơ vẫn còn (RB-11)
        getJson("/api/staff/pets/" + petId, STAFF, 200);
    }

    @Test
    void lostCancelsVetRequestsAndFoundCreatesIntake() throws Exception {
        long petId = rescuedPet("CAT", LocalDate.now().minusMonths(6));
        long vetId = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", 1, "reason", "Khám mắt", "priority", "NEED_CHECK", "careMode", "INPATIENT"), 201)
            .get("request").get("id").asLong();
        assertThat(petStatus(petId)).isEqualTo("IN_TREATMENT");

        JsonNode noDetail = postJson("/api/staff/pets/" + petId + "/end-events", STAFF, map("eventType", "LOST"), 400);
        assertThat(noDetail.get("code").asText()).isEqualTo("DETAIL_REQUIRED");

        JsonNode lost = postJson("/api/staff/pets/" + petId + "/end-events", STAFF,
            map("eventType", "LOST", "detail", "Trốn khỏi phòng khám chiều 1/11"), 201);
        assertThat(lost.get("cancelledVetRequests").asInt()).isEqualTo(1);
        assertThat(getJson("/api/staff/vet-requests/" + vetId, STAFF, 200).get("status").asText()).isEqualTo("CANCELLED");

        JsonNode found = postJson("/api/staff/pets/" + petId + "/found", STAFF,
            map("reason", "Người dân báo thấy ở chợ", "priority", "NEED_CHECK"), 200);
        assertThat(found.get("petStatus").asText()).isEqualTo("IN_CARE");
        JsonNode pet = getJson("/api/staff/pets/" + petId, STAFF, 200);
        assertThat(pet.get("area").asText()).isEqualTo("QUARANTINE");
        assertThat(pet.get("currentLocation").asText()).isEqualTo("Trung tâm");
        assertThat(pet.get("intakes").findValuesAsText("source")).containsExactly("FOUND", "RESCUE");

        // Chỉ thú đang Thất lạc mới tìm lại
        postJson("/api/staff/pets/" + petId + "/found", STAFF, map("reason", "x", "priority", "STABLE"), 409);
    }

    @Test
    void transferNeedsDestinationAndReason() throws Exception {
        long petId = rescuedPet("DOG", LocalDate.now().minusYears(1));
        JsonNode bad = postJson("/api/staff/pets/" + petId + "/end-events", STAFF,
            map("eventType", "TRANSFERRED", "detail", "Quá tải"), 400);
        assertThat(bad.get("code").asText()).isEqualTo("DESTINATION_REQUIRED");
        JsonNode ok = postJson("/api/staff/pets/" + petId + "/end-events", STAFF, map("eventType", "TRANSFERRED",
            "detail", "Trung tâm quá tải", "destinationOrg", "Trạm cứu hộ Thủ Đức"), 201);
        assertThat(ok.get("event").get("destinationOrg").asText()).isEqualTo("Trạm cứu hộ Thủ Đức");
        assertThat(getJson("/api/staff/pets/" + petId, STAFF, 200).get("currentLocation").asText())
            .isEqualTo("Trạm cứu hộ Thủ Đức");
    }

    @Test
    void adoptedPetOnlyDuringFollowUpPeriod() throws Exception {
        JsonNode handover = handedOver(ADOPTER3);
        long petId = handover.get("petId").asLong();
        long handoverId = handover.get("id").asLong();

        JsonNode transfer = postJson("/api/staff/pets/" + petId + "/end-events", STAFF,
            map("eventType", "TRANSFERRED", "detail", "x", "destinationOrg", "y"), 409);
        assertThat(transfer.get("code").asText()).isEqualTo("INVALID_END_EVENT");

        JsonNode lost = postJson("/api/staff/pets/" + petId + "/end-events", STAFF,
            map("eventType", "LOST", "detail", "Chủ nuôi báo bé đi lạc khi dắt đi dạo"), 201);
        assertThat(lost.get("handoverClosed").asBoolean()).isTrue();
        assertThat(lost.get("cancelledFollowUps").asInt()).isEqualTo(2);
        JsonNode after = getJson("/api/staff/pets/" + petId + "/handovers", STAFF, 200).get(0);
        assertThat(after.get("id").asLong()).isEqualTo(handoverId);
        assertThat(after.get("status").asText()).isEqualTo("CLOSED");
        assertThat(after.get("followUps").findValuesAsText("status")).containsOnly("CANCELLED");
    }

    @Test
    void adoptedAfterFollowUpsCompletedIsRejected() throws Exception {
        JsonNode handover = handedOver(ADOPTER1);
        long petId = handover.get("petId").asLong();
        for (JsonNode f : handover.get("followUps")) {
            postJson("/api/staff/follow-ups/" + f.get("id").asLong() + "/result", STAFF,
                HandoverFlowIntegrationTest.criteria("PASS", null), 200);
        }
        JsonNode res = postJson("/api/staff/pets/" + petId + "/end-events", STAFF,
            map("eventType", "DECEASED", "detail", "Tuổi già"), 409);
        assertThat(res.get("code").asText()).isEqualTo("FOLLOW_UP_PERIOD_ENDED");
        assertThat(petStatus(petId)).isEqualTo("ADOPTED");
        postJson("/api/staff/pets/" + petId + "/end-events", ADOPTER1, map("eventType", "DECEASED"), 403);
    }
}
