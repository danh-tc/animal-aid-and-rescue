package com.aar.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** UC-09: chăm sóc, yêu cầu thú y, checklist (người B, báo cáo mục 2.1.3). */
class CareFlowIntegrationTest extends IntegrationTestBase {

    private static final Object CHECKLIST_ALL = map("vaccinated", true, "dewormed", true, "sterilized", true,
        "temperamentOk", true);

    /** QĐ-05, QĐ-07: chưa qua thú y và chưa hết cách ly thì không lên Sẵn sàng, báo các mục còn thiếu. */
    @Test
    void checklistReportsMissingItemsAndKeepsPetInCare() throws Exception {
        long petId = rescuedPet("DOG", LocalDate.now().minusDays(400));
        JsonNode result = putJson("/api/staff/pets/" + petId + "/eligibility", STAFF, CHECKLIST_ALL, 200);
        assertThat(result.get("status").asText()).isEqualTo("IN_CARE");
        assertThat(texts(result.get("missing"))).containsExactlyInAnyOrder(
            "Sức khỏe ổn định", "Hết cách ly, tính cách không hung dữ");
    }

    /** QĐ-06: mở yêu cầu thú y => Đang điều trị, lưu trú đổi nơi ở; đóng yêu cầu cuối cùng => Đang chăm sóc. */
    @Test
    void vetRequestsDriveTreatmentStatus() throws Exception {
        long petId = rescuedPet("CAT", LocalDate.now().minusDays(300));
        JsonNode first = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", 2, "reason", "Nhiễm trùng mắt", "priority", "URGENT", "careMode", "INPATIENT"), 201);
        assertThat(first.get("petStatus").asText()).isEqualTo("IN_TREATMENT");
        assertThat(first.get("request").get("status").asText()).isEqualTo("PENDING");
        assertThat(first.get("request").get("clinicName").asText()).isEqualTo("Bệnh viện thú y Bình Minh");
        assertThat(getJson("/api/staff/pets/" + petId, STAFF, 200).get("currentLocation").asText())
            .isEqualTo("Bệnh viện thú y Bình Minh");

        JsonNode second = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", 1, "reason", "Tiêm phòng", "priority", "STABLE", "careMode", "OUTPATIENT"), 201);
        long firstId = first.get("request").get("id").asLong();
        long secondId = second.get("request").get("id").asLong();

        postJson("/api/staff/vet-requests/" + firstId + "/start", STAFF, null, 200);
        // Hoàn tất phải có chi phí (RB-07)
        postJson("/api/staff/vet-requests/" + firstId + "/complete", STAFF, map("diagnosisSummary", "Ổn"), 400);
        JsonNode done = postJson("/api/staff/vet-requests/" + firstId + "/complete", STAFF,
            map("diagnosisSummary", "Viêm kết mạc", "totalCost", 750000, "aftercareInstructions", "Nhỏ mắt"), 200);
        assertThat(done.get("request").get("status").asText()).isEqualTo("COMPLETED");
        assertThat(done.get("petStatus").asText()).isEqualTo("IN_TREATMENT");

        postJson("/api/staff/vet-requests/" + secondId + "/cancel", STAFF, map("reason", ""), 400);
        JsonNode cancelled = postJson("/api/staff/vet-requests/" + secondId + "/cancel", STAFF,
            map("reason", "Phòng khám hết vaccine"), 200);
        assertThat(cancelled.get("petStatus").asText()).isEqualTo("IN_CARE");
        assertThat(getJson("/api/staff/pets/" + petId, STAFF, 200).get("currentLocation").asText()).isEqualTo("Trung tâm");

        // Yêu cầu đã đóng không đổi được nữa
        postJson("/api/staff/vet-requests/" + firstId + "/cancel", STAFF, map("reason", "x"), 409);
        assertThat(getJson("/api/staff/pets/" + petId + "/vet-requests", STAFF, 200)).hasSize(2);
    }

    @Test
    void inactiveClinicCannotReceiveRequests() throws Exception {
        long petId = rescuedPet("DOG", LocalDate.now().minusDays(300));
        JsonNode clinic = postJson("/api/admin/vet-clinics", MANAGER,
            map("name", "Phòng khám ngừng hợp tác", "active", false), 201);
        JsonNode error = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", clinic.get("id").asLong(), "reason", "Khám", "priority", "STABLE", "careMode", "OUTPATIENT"), 400);
        assertThat(error.get("code").asText()).isEqualTo("INVALID_CLINIC");
    }

    /** RB-13: đủ 6 mục thì chuyển Sẵn sàng, tự chuyển khu chung, ghi người đánh giá. */
    @Test
    void fullChecklistMovesPetToAvailable() throws Exception {
        long petId = availablePet(false);
        JsonNode pet = getJson("/api/staff/pets/" + petId, STAFF, 200);
        assertThat(pet.get("status").asText()).isEqualTo("AVAILABLE");
        assertThat(pet.get("area").asText()).isEqualTo("GENERAL");
        assertThat(pet.get("eligibilityCheckedBy").asLong()).isEqualTo(STAFF_ID);
        assertThat(pet.get("checklist").get("allPassed").asBoolean()).isTrue();
        assertThat(getJson("/api/public/pets/" + petId, null, 200).get("sterilized").asBoolean()).isTrue();

        // Thú Sẵn sàng không sửa checklist trực tiếp, phải đánh dấu không đủ điều kiện
        putJson("/api/staff/pets/" + petId + "/eligibility", STAFF, CHECKLIST_ALL, 409);
    }

    /** Thú đủ tuổi triệt sản nhưng chưa triệt sản thì không đạt mục 5. */
    @Test
    void adultUnsterilizedPetFailsSterilizationItem() throws Exception {
        long petId = rescuedPet("DOG", LocalDate.now().minusDays(400));
        JsonNode vet = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", 1, "reason", "Khám", "priority", "STABLE", "careMode", "OUTPATIENT"), 201);
        postJson("/api/staff/vet-requests/" + vet.get("request").get("id").asLong() + "/complete", STAFF,
            map("totalCost", 0), 200);
        backdateIntakes(petId, 30);
        JsonNode result = putJson("/api/staff/pets/" + petId + "/eligibility", STAFF,
            map("vaccinated", true, "dewormed", true, "sterilized", false, "temperamentOk", true), 200);
        assertThat(result.get("status").asText()).isEqualTo("IN_CARE");
        assertThat(texts(result.get("missing"))).containsExactly("Triệt sản hoặc chưa đủ tuổi triệt sản");
    }

    /** QĐ-10: không đủ điều kiện khi Sẵn sàng => Đang chăm sóc, hủy đơn đang chờ. */
    @Test
    void markingIneligibleCancelsPendingApplications() throws Exception {
        long petId = availablePet(false);
        long app1 = submitApplication(petId, ADOPTER1, false);
        long app2 = submitApplication(petId, ADOPTER2, false);
        JsonNode result = postJson("/api/staff/pets/" + petId + "/ineligible", STAFF,
            map("reason", "Phát hiện hung dữ với trẻ em"), 200);
        assertThat(result.get("status").asText()).isEqualTo("IN_CARE");
        assertThat(result.get("cancelledApplications").asInt()).isEqualTo(2);
        assertThat(applicationStatus(app1)).isEqualTo("CANCELLED");
        assertThat(applicationStatus(app2)).isEqualTo("CANCELLED");
        getJson("/api/public/pets/" + petId, null, 404);
        // Đang chăm sóc thì không đánh dấu không đủ điều kiện nữa
        postJson("/api/staff/pets/" + petId + "/ineligible", STAFF, map("reason", "x"), 409);
    }

    /** Ngoại lệ 3a của UC-09: thú Sẵn sàng bị bệnh => Đang điều trị, đơn đang chờ bị hủy. */
    @Test
    void sickAvailablePetGoesToTreatmentAndCancelsApplications() throws Exception {
        long petId = availablePet(false);
        long app = submitApplication(petId, ADOPTER3, false);
        JsonNode result = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", 1, "reason", "Bỏ ăn", "priority", "NEED_CHECK", "careMode", "OUTPATIENT"), 201);
        assertThat(result.get("petStatus").asText()).isEqualTo("IN_TREATMENT");
        assertThat(result.get("cancelledApplications").asInt()).isEqualTo(1);
        assertThat(applicationStatus(app)).isEqualTo("CANCELLED");
    }

    @Test
    void careLogsAndAreaChange() throws Exception {
        long petId = rescuedPet("DOG", LocalDate.now().minusDays(300));
        postJson("/api/staff/pets/" + petId + "/care-logs", STAFF, map("activityType", "WEIGHT"), 400);
        postJson("/api/staff/pets/" + petId + "/care-logs", STAFF,
            map("activityType", "WEIGHT", "weightKg", 7.5, "note", "Tăng cân"), 201);
        postJson("/api/staff/pets/" + petId + "/care-logs", STAFF, map("activityType", "FEEDING"), 201);
        JsonNode updated = patchJson("/api/staff/pets/" + petId, STAFF,
            map("area", "GENERAL", "color", "Vàng nhạt", "status", "AVAILABLE"), 200);
        assertThat(updated.get("area").asText()).isEqualTo("GENERAL");
        // Không có cách đổi trạng thái qua PATCH (RB-12)
        assertThat(updated.get("status").asText()).isEqualTo("IN_CARE");

        JsonNode logs = getJson("/api/staff/pets/" + petId + "/care-logs", STAFF, 200);
        assertThat(logs.get("totalItems").asInt()).isEqualTo(3);
        assertThat(logs.get("items").get(0).get("activityType").asText()).isEqualTo("AREA_CHANGE");
    }

    @Test
    void historyRecordsEveryTransition() throws Exception {
        long petId = availablePet(false);
        JsonNode history = getJson("/api/staff/pets/" + petId + "/history", STAFF, 200);
        assertThat(texts(history, "event")).containsExactly(
            "RESCUE_SUCCEEDED", "VET_REQUEST_OPENED", "VET_REQUEST_CLOSED", "ELIGIBILITY_PASSED");
    }

    private static List<String> texts(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.asText()));
        return out;
    }

    private static List<String> texts(JsonNode array, String field) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get(field).asText()));
        return out;
    }
}
