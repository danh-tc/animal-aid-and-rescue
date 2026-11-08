package com.aar.rescue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** UC-07: báo cứu hộ, xử lý báo cáo và ca (người A, báo cáo mục 2.1.2). */
class RescueFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void guestSubmitsReportWithPhotoAndGetsCode() throws Exception {
        String body = json.writeValueAsString(map("reporterName", "Lan", "reporterPhone", randomPhone(),
            "locationText", "Chợ Bến Thành", "description", "Mèo bị thương"));
        JsonNode receipt = call(multipart("/api/public/rescue-reports")
            .file(new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE, body.getBytes()))
            .file(new MockMultipartFile("photos", "meo.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3})),
            null, 201);
        String code = receipt.get("code").asText();
        assertThat(code).matches("BC-[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}");
        assertThat(receipt.get("status").asText()).isEqualTo("NEW");

        JsonNode detail = getJson("/api/staff/rescue-reports/" + reportId(code), STAFF, 200);
        assertThat(detail.get("photos")).hasSize(1);
        long photoId = detail.get("photos").get(0).get("id").asLong();
        byte[] downloaded = mvc.perform(get("/api/staff/attachments/" + photoId)
                .header("Authorization", "Bearer " + token(STAFF)))
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsNonImageUpload() throws Exception {
        String body = json.writeValueAsString(map("reporterName", "Lan", "reporterPhone", randomPhone(),
            "locationText", "Q1", "description", "Chó"));
        JsonNode error = call(multipart("/api/public/rescue-reports")
            .file(new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE, body.getBytes()))
            .file(new MockMultipartFile("photos", "x.exe", "application/octet-stream", new byte[]{1})), null, 415);
        assertThat(error.get("code").asText()).isEqualTo("UNSUPPORTED_FILE_TYPE");
    }

    @Test
    void invalidReportIsRejectedByValidation() throws Exception {
        JsonNode error = postJson("/api/public/rescue-reports", null,
            map("reporterName", "", "reporterPhone", "abc", "locationText", "Q1", "description", "x"), 400);
        assertThat(error.get("fieldErrors").has("reporterPhone")).isTrue();
        assertThat(error.get("fieldErrors").has("reporterName")).isTrue();
    }

    @Test
    void sameReporterIsRateLimitedPerHour() throws Exception {
        String phone = randomPhone();
        var body = map("reporterName", "Spam", "reporterPhone", phone, "locationText", "Q1", "description", "x");
        for (int i = 0; i < 5; i++) {
            postJson("/api/public/rescue-reports", null, body, 201);
        }
        JsonNode error = postJson("/api/public/rescue-reports", null, body, 429);
        assertThat(error.get("code").asText()).isEqualTo("RATE_LIMITED");
    }

    /** QĐ-01: báo cáo có hai thú tách thành hai ca. Báo cáo chuyển Đã xử lý. */
    @Test
    void reportWithTwoAnimalsIsSplitIntoTwoCases() throws Exception {
        long reportId = reportId(submitReport());
        JsonNode cases = postJson("/api/staff/rescue-reports/" + reportId + "/cases", STAFF,
            map("numberOfAnimals", 2), 201);
        assertThat(cases).hasSize(2);
        assertThat(cases.get(0).get("status").asText()).isEqualTo("NEW");

        JsonNode detail = getJson("/api/staff/rescue-reports/" + reportId, STAFF, 200);
        assertThat(detail.get("status").asText()).isEqualTo("PROCESSED");
        assertThat(detail.get("cases")).hasSize(2);

        // Báo cáo đã xử lý không tạo ca lần nữa (RB-12)
        JsonNode error = postJson("/api/staff/rescue-reports/" + reportId + "/cases", STAFF,
            map("numberOfAnimals", 1), 409);
        assertThat(error.get("code").asText()).isEqualTo("INVALID_TRANSITION");
    }

    @Test
    void spamReportIsMarkedInvalid() throws Exception {
        long reportId = reportId(submitReport());
        postJson("/api/staff/rescue-reports/" + reportId + "/invalid", STAFF, map("reason", ""), 400);
        JsonNode report = postJson("/api/staff/rescue-reports/" + reportId + "/invalid", STAFF,
            map("reason", "Nội dung quảng cáo"), 200);
        assertThat(report.get("status").asText()).isEqualTo("INVALID");
    }

    /** S-1 của UC-07: tạo hồ sơ thú IN_CARE ở khu cách ly và lượt tiếp nhận RESCUE, có lịch sử trạng thái. */
    @Test
    void successfulCaseCreatesPetAndRescueIntake() throws Exception {
        long reportId = reportId(submitReport());
        long caseId = postJson("/api/staff/rescue-reports/" + reportId + "/cases", STAFF,
            map("numberOfAnimals", 1), 201).get(0).get("id").asLong();

        // Chưa phân công thì chưa ghi kết quả được
        postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF, succeedBody("URGENT"), 409);
        // Chỉ Quản lý được phân công, và chỉ phân công cho nhân viên
        postJson("/api/staff/rescue-cases/" + caseId + "/assign", STAFF, map("staffId", 3), 403);
        postJson("/api/staff/rescue-cases/" + caseId + "/assign", MANAGER, map("staffId", 4), 400);
        JsonNode assigned = postJson("/api/staff/rescue-cases/" + caseId + "/assign", MANAGER, map("staffId", 3), 200);
        assertThat(assigned.get("status").asText()).isEqualTo("IN_PROGRESS");

        // QĐ-17: nhân viên khác xem được ca nhưng không ghi kết quả được
        getJson("/api/staff/rescue-cases/" + caseId, STAFF, 200);
        JsonNode notMine = postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF, succeedBody("URGENT"), 403);
        assertThat(notMine.get("code").asText()).isEqualTo("CASE_NOT_ASSIGNEE");
        postJson("/api/staff/rescue-cases/" + caseId + "/fail", STAFF, map("reason", "Không thấy"), 403);

        // Quản lý phân công lại, ca vẫn Đang xử lý, người cũ hết quyền ghi kết quả
        postJson("/api/staff/rescue-cases/" + caseId + "/assign", MANAGER, map("staffId", 3), 400);
        JsonNode reassigned = postJson("/api/staff/rescue-cases/" + caseId + "/assign", MANAGER, map("staffId", STAFF_ID), 200);
        assertThat(reassigned.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(reassigned.get("assignedStaffId").asLong()).isEqualTo(STAFF_ID);
        postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF2, succeedBody("URGENT"), 403);

        JsonNode result = postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF, succeedBody("URGENT"), 200);
        assertThat(result.get("rescueCase").get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(result.get("petCode").asText()).matches("TH-\\d{4}-\\d{4,}");
        assertThat(result.get("urgentVetRequestSuggested").asBoolean()).isTrue();

        long petId = result.get("petId").asLong();
        JsonNode pet = getJson("/api/staff/pets/" + petId, STAFF, 200);
        assertThat(pet.get("status").asText()).isEqualTo("IN_CARE");
        assertThat(pet.get("area").asText()).isEqualTo("QUARANTINE");
        assertThat(pet.get("intakes")).hasSize(1);
        assertThat(pet.get("intakes").get(0).get("source").asText()).isEqualTo("RESCUE");
        assertThat(pet.get("intakes").get(0).get("rescueCaseId").asLong()).isEqualTo(caseId);

        JsonNode history = getJson("/api/staff/pets/" + petId + "/history", STAFF, 200);
        assertThat(history.get(0).get("event").asText()).isEqualTo("RESCUE_SUCCEEDED");
        assertThat(history.get(0).get("changedBy").asLong()).isEqualTo(STAFF_ID);

        JsonNode caseDetail = getJson("/api/staff/rescue-cases/" + caseId, STAFF, 200);
        assertThat(caseDetail.get("petId").asLong()).isEqualTo(petId);
        // Ca đã đóng không ghi kết quả lần hai (RB-03)
        postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF, succeedBody("STABLE"), 409);
    }

    /** QĐ-17: Quản lý được ghi kết quả thay người phụ trách. */
    @Test
    void managerCanCloseCaseOnBehalfOfAssignee() throws Exception {
        long reportId = reportId(submitReport());
        long caseId = postJson("/api/staff/rescue-reports/" + reportId + "/cases", MANAGER,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 201).get(0).get("id").asLong();
        JsonNode failed = postJson("/api/staff/rescue-cases/" + caseId + "/fail", MANAGER,
            map("reason", "Người phụ trách báo qua điện thoại: không tìm thấy"), 200);
        assertThat(failed.get("status").asText()).isEqualTo("FAILED");
        String events = jdbc.queryForObject(
            "SELECT string_agg(event, ',' ORDER BY id) FROM status_histories WHERE entity_type = 'RESCUE_CASE' AND entity_id = ?",
            String.class, caseId);
        assertThat(events).isEqualTo("CREATED,ASSIGNED,FAILED");
    }

    /** S-2 của UC-07: thất bại bắt buộc lý do, không tạo hồ sơ thú (QĐ-02). */
    @Test
    void failedCaseRequiresReasonAndCreatesNoPet() throws Exception {
        long reportId = reportId(submitReport());
        long caseId = postJson("/api/staff/rescue-reports/" + reportId + "/cases", MANAGER,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 201).get(0).get("id").asLong();
        postJson("/api/staff/rescue-cases/" + caseId + "/fail", STAFF, map("reason", " "), 400);
        JsonNode failed = postJson("/api/staff/rescue-cases/" + caseId + "/fail", STAFF,
            map("reason", "Thú chạy mất"), 200);
        assertThat(failed.get("status").asText()).isEqualTo("FAILED");
        assertThat(failed.get("failureReason").asText()).isEqualTo("Thú chạy mất");
        assertThat(getJson("/api/staff/rescue-cases/" + caseId, STAFF, 200).get("petId").isNull()).isTrue();
    }

    @Test
    void adopterCannotProcessReports() throws Exception {
        getJson("/api/staff/rescue-reports", ADOPTER1, 403);
        getJson("/api/staff/rescue-reports?status=NEW", MANAGER, 200);
    }

    @Test
    void createdPetIsListedForStaffSearch() throws Exception {
        long petId = rescuedPet("CAT", LocalDate.now().minusDays(60));
        JsonNode code = getJson("/api/staff/pets/" + petId, STAFF, 200).get("code");
        JsonNode list = getJson("/api/staff/pets?status=IN_CARE&species=CAT&q=" + code.asText(), STAFF, 200);
        assertThat(list.get("totalItems").asInt()).isEqualTo(1);
    }

    private static Object succeedBody(String priority) {
        return map("pet", map("name", "Cún", "species", "DOG", "sex", "FEMALE"),
            "intake", map("priority", priority, "initialAssessment", "Chân sau chảy máu"));
    }

    /** Ma trận phân quyền mục 5.2: chỉ Quản lý phân công. Nhân viên vẫn tạo ca chưa phân công được. */
    @Test
    void onlyManagerAssignsWhenCreatingCases() throws Exception {
        long reportId = reportId(submitReport());
        JsonNode denied = postJson("/api/staff/rescue-reports/" + reportId + "/cases", STAFF,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 403);
        assertThat(denied.get("code").asText()).isEqualTo("ASSIGN_MANAGER_ONLY");
        assertThat(getJson("/api/staff/rescue-reports/" + reportId, STAFF, 200).get("status").asText()).isEqualTo("NEW");

        JsonNode created = postJson("/api/staff/rescue-reports/" + reportId + "/cases", STAFF,
            map("numberOfAnimals", 1), 201);
        long caseId = created.get(0).get("id").asLong();
        assertThat(created.get(0).get("status").asText()).isEqualTo("NEW");
        postJson("/api/staff/rescue-cases/" + caseId + "/assign", STAFF, map("staffId", STAFF_ID), 403);
        JsonNode assigned = postJson("/api/staff/rescue-cases/" + caseId + "/assign", MANAGER, map("staffId", STAFF_ID), 200);
        assertThat(assigned.get("assignedStaffId").asLong()).isEqualTo(STAFF_ID);
        // Quản lý tự phân công cho mình cũng được
        long other = postJson("/api/staff/rescue-reports/" + reportId(submitReport()) + "/cases", MANAGER,
            map("numberOfAnimals", 1, "assignedStaffId", 1), 201).get(0).get("assignedStaffId").asLong();
        assertThat(other).isEqualTo(1L);
    }

    /** S-1 của UC-07: hồ sơ thú gồm loài, đặc điểm và ảnh, tạo trong cùng một yêu cầu. */
    @Test
    void succeedWithPhotosStoresPetPhotos() throws Exception {
        long reportId = reportId(submitReport());
        long caseId = postJson("/api/staff/rescue-reports/" + reportId + "/cases", MANAGER,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 201).get(0).get("id").asLong();
        String data = json.writeValueAsString(succeedBody("STABLE"));
        JsonNode result = call(multipart("/api/staff/rescue-cases/" + caseId + "/succeed")
            .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, data.getBytes()))
            .file(new MockMultipartFile("photos", "luc-cuu.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3}))
            .file(new MockMultipartFile("photos", "luc-cuu-2.png", MediaType.IMAGE_PNG_VALUE, new byte[]{4, 5, 6})),
            STAFF, 200);
        JsonNode pet = getJson("/api/staff/pets/" + result.get("petId").asLong(), STAFF, 200);
        assertThat(pet.get("photos").findValuesAsText("originalName")).containsExactly("luc-cuu.jpg", "luc-cuu-2.png");

        // Tệp sai định dạng thì cả giao dịch hủy: ca vẫn Đang xử lý, không tạo hồ sơ thú
        long other = postJson("/api/staff/rescue-reports/" + reportId(submitReport()) + "/cases", MANAGER,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 201).get(0).get("id").asLong();
        call(multipart("/api/staff/rescue-cases/" + other + "/succeed")
            .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, data.getBytes()))
            .file(new MockMultipartFile("photos", "virus.exe", "application/octet-stream", new byte[]{1})),
            STAFF, 415);
        assertThat(getJson("/api/staff/rescue-cases/" + other, STAFF, 200).get("status").asText()).isEqualTo("IN_PROGRESS");
    }
}
