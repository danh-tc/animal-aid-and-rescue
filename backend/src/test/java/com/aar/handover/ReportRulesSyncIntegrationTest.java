package com.aar.handover;

import static com.aar.handover.HandoverFlowIntegrationTest.criteria;
import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportRulesSyncIntegrationTest extends IntegrationTestBase {

    @Test
    void staffCancelsApprovedApplicationWhenAdopterDoesNotShowUp() throws Exception {
        long appId = approvedApplication(ADOPTER1);
        long petId = petIdOfApplication(appId);

        JsonNode noReason = postJson("/api/staff/applications/" + appId + "/cancel", STAFF, map("reason", " "), 400);
        assertThat(noReason.get("code").asText()).isNotBlank();

        JsonNode cancelled = postJson("/api/staff/applications/" + appId + "/cancel", STAFF,
            map("reason", "Không đến theo hẹn, gọi 3 lần không được"), 200);
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("cancelReason").asText()).contains("Không đến theo hẹn");
        assertThat(petStatus(petId)).isEqualTo("AVAILABLE");

        long pending = submitApplication(petId, ADOPTER2);
        JsonNode notApproved = postJson("/api/staff/applications/" + pending + "/cancel", STAFF,
            map("reason", "x"), 409);
        assertThat(notApproved.get("code").asText()).isEqualTo("APPLICATION_NOT_APPROVED");
    }

    @Test
    void additionalFollowUpOnlyPassOrFail() throws Exception {
        JsonNode handover = handedOver(ADOPTER2);
        long first = handover.get("followUps").get(0).get("id").asLong();

        JsonNode watch = postJson("/api/staff/follow-ups/" + first + "/result", STAFF,
            criteria("WATCH", "Chưa liên lạc được chủ"), 200);
        long extra = watch.get("additionalFollowUp").get("id").asLong();

        JsonNode watchAgain = postJson("/api/staff/follow-ups/" + extra + "/result", STAFF,
            criteria("WATCH", "Vẫn chưa ổn"), 400);
        assertThat(watchAgain.get("code").asText()).isEqualTo("ADDITIONAL_RESULT_INVALID");

        @SuppressWarnings("unchecked")
        Map<String, Object> unreachable = (Map<String, Object>) criteria("FAIL", "Lần bổ sung vẫn không liên lạc được chủ");
        unreachable.put("ownerReachable", false);
        JsonNode fail = postJson("/api/staff/follow-ups/" + extra + "/result", STAFF, unreachable, 200);
        assertThat(fail.get("handoverStatus").asText()).isEqualTo("RECALL_PENDING");
    }

    @Test
    void managerClosesRecallAfterWaitingPeriod() throws Exception {
        JsonNode handover = handedOver(ADOPTER3);
        long handoverId = handover.get("id").asLong();
        long petId = handover.get("petId").asLong();
        long first = handover.get("followUps").get(0).get("id").asLong();
        postJson("/api/staff/follow-ups/" + first + "/result", STAFF, criteria("FAIL", "Chủ cho thú cho người khác"), 200);

        JsonNode staff = postJson("/api/staff/handovers/" + handoverId + "/close-recall", STAFF,
            map("reason", "Không liên lạc được"), 403);
        assertThat(staff.get("code").asText()).isEqualTo("CLOSE_RECALL_MANAGER_ONLY");

        JsonNode early = postJson("/api/staff/handovers/" + handoverId + "/close-recall", MANAGER,
            map("reason", "Không liên lạc được"), 409);
        assertThat(early.get("code").asText()).isEqualTo("RECALL_CLOSE_TOO_EARLY");

        jdbc.update("UPDATE follow_ups SET performed_at = now() - interval '31 days' WHERE id = ?", first);
        JsonNode exceptions = getJson("/api/admin/reports/exceptions", MANAGER, 200);
        assertThat(exceptions.get("recallPending").findValuesAsText("handoverId")).contains(Long.toString(handoverId));

        JsonNode closed = postJson("/api/staff/handovers/" + handoverId + "/close-recall", MANAGER,
            map("reason", "Đã liên hệ nhiều lần, chủ chuyển đi nơi khác"), 200);
        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        assertThat(petStatus(petId)).isEqualTo("ADOPTED");

        long another = availablePet(false);
        long appId = submitApplication(another, ADOPTER3);
        JsonNode detail = getJson("/api/staff/applications/" + appId, STAFF, 200);
        assertThat(detail.get("applicantHistory").findValuesAsText("kind")).contains("RECALLED");
        postJson("/api/staff/applications/" + appId + "/approve", STAFF, null, 200);
    }

    @Test
    void rescueOfLostPetCreatesFoundIntakeOnExistingRecord() throws Exception {
        long petId = rescuedPet("CAT", LocalDate.now().minusYears(2));
        postJson("/api/staff/pets/" + petId + "/end-events", STAFF,
            map("eventType", "LOST", "detail", "Sổng chuồng khi dọn vệ sinh"), 201);

        long reportId = reportId(submitReport());
        long caseId = postJson("/api/staff/rescue-reports/" + reportId + "/cases", MANAGER,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 201).get(0).get("id").asLong();

        JsonNode both = postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF, map(
            "pet", map("species", "CAT"), "lostPetId", petId,
            "intake", map("priority", "STABLE")), 400);
        assertThat(both.get("code").asText()).isEqualTo("PET_OR_LOST_PET_REQUIRED");

        JsonNode result = postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF, map(
            "lostPetId", petId, "intake", map("priority", "NEED_CHECK", "reason", "Người dân báo thấy ở chợ")), 200);
        assertThat(result.get("petId").asLong()).isEqualTo(petId);
        assertThat(petStatus(petId)).isEqualTo("IN_CARE");
        assertThat(jdbc.queryForObject("SELECT source FROM intakes WHERE rescue_case_id = ?", String.class, caseId))
            .isEqualTo("FOUND");
    }

    @Test
    void returnedPetNeedsNewVetExamBeforeAvailable() throws Exception {
        JsonNode handover = handedOver(ADOPTER1);
        long petId = handover.get("petId").asLong();
        postJson("/api/staff/pets/" + petId + "/returns", STAFF,
            map("reason", "Chủ chuyển nhà", "priority", "STABLE"), 201);

        jdbc.update("UPDATE vet_requests SET completed_at = now() - interval '200 days' WHERE pet_id = ?", petId);
        jdbc.update("UPDATE intakes SET intake_at = now() - interval '20 days' WHERE pet_id = ? AND source = 'RETURN'", petId);

        JsonNode check = putJson("/api/staff/pets/" + petId + "/eligibility", STAFF,
            map("vaccinated", true, "dewormed", true, "sterilized", true, "temperamentOk", true), 200);
        assertThat(check.get("status").asText()).isEqualTo("IN_CARE");
    }

    @Test
    void reportCoordinatesMustBeComplete() throws Exception {
        Map<String, Object> base = map("reporterName", "Người báo", "reporterPhone", randomPhone(),
            "locationText", "Quận 1", "description", "Chó đi lạc");
        Map<String, Object> half = new java.util.LinkedHashMap<>(base);
        half.put("latitude", 10.77);
        JsonNode bad = postJson("/api/public/rescue-reports", null, half, 400);
        assertThat(bad.get("code").asText()).isEqualTo("INVALID_COORDINATES");
        Map<String, Object> full = new java.util.LinkedHashMap<>(base);
        full.put("latitude", 10.77);
        full.put("longitude", 106.70);
        postJson("/api/public/rescue-reports", null, full, 201);
    }

    @Test
    void unassignedCaseAppearsInExceptionReport() throws Exception {
        long reportId = reportId(submitReport());
        long caseId = postJson("/api/staff/rescue-reports/" + reportId + "/cases", STAFF,
            map("numberOfAnimals", 1), 201).get(0).get("id").asLong();
        jdbc.update("UPDATE rescue_cases SET created_at = now() - interval '3 hours' WHERE id = ?", caseId);

        JsonNode exceptions = getJson("/api/admin/reports/exceptions", MANAGER, 200);
        assertThat(exceptions.get("caseUnassignedAlertHours").asInt()).isEqualTo(2);
        assertThat(exceptions.get("unassignedCases").findValuesAsText("caseId")).contains(Long.toString(caseId));
    }
}
