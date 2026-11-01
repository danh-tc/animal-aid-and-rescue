package com.aar.rescue;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/** Đợt 2 của UC-07 và P04: tra cứu báo cáo (RB-15), ca đang mở ở gần, gộp báo cáo trùng (QĐ-01). */
class RescuePhase2IntegrationTest extends IntegrationTestBase {

    @Test
    void lookupNeedsBothCodeAndPhone() throws Exception {
        String phone = randomPhone();
        String code = postJson("/api/public/rescue-reports", null, map("reporterName", "Hà", "reporterPhone", phone,
            "locationText", "Quận 5", "description", "Mèo kẹt trên mái"), 201).get("code").asText();

        JsonNode found = getJson("/api/public/rescue-reports/lookup?code=" + code.toLowerCase() + "&phone=" + phone, null, 200);
        assertThat(found.get("code").asText()).isEqualTo(code);
        assertThat(found.get("status").asText()).isEqualTo("NEW");
        assertThat(found.get("cases")).isEmpty();
        // Không lộ dữ liệu nội bộ
        assertThat(found.has("reporterName")).isFalse();
        assertThat(found.has("description")).isFalse();

        JsonNode wrong = getJson("/api/public/rescue-reports/lookup?code=" + code + "&phone=0900000999", null, 404);
        assertThat(wrong.get("message").asText()).contains("mã và số điện thoại");

        postJson("/api/staff/rescue-reports/" + reportId(code) + "/cases", STAFF,
            map("numberOfAnimals", 2, "assignedStaffId", STAFF_ID), 201);
        JsonNode processed = getJson("/api/public/rescue-reports/lookup?code=" + code + "&phone=" + phone, null, 200);
        assertThat(processed.get("status").asText()).isEqualTo("PROCESSED");
        assertThat(processed.get("cases").findValuesAsText("status")).containsExactly("IN_PROGRESS", "IN_PROGRESS");
    }

    @Test
    void openCasesSortedByDistanceAndMergeMarksDuplicate() throws Exception {
        // Ca A ở gần (cách khoảng 0,5 km), ca B ở xa (Hà Nội)
        long nearCase = caseAt(10.7769, 106.7009);
        long farCase = caseAt(21.0285, 105.8542);

        String dupCode = reportAt(10.7800, 106.7040);
        long dupId = reportId(dupCode);
        JsonNode detail = getJson("/api/staff/rescue-reports/" + dupId, STAFF, 200);
        JsonNode open = detail.get("openCases");
        assertThat(open.get(0).get("rescueCase").get("id").asLong()).isEqualTo(nearCase);
        assertThat(open.get(0).get("distanceKm").asDouble()).isLessThan(1.0);
        int farIndex = open.findValuesAsText("id").indexOf(Long.toString(farCase));
        assertThat(farIndex).isGreaterThan(0);

        JsonNode merged = postJson("/api/staff/rescue-reports/" + dupId + "/merge", STAFF, map("caseId", nearCase), 200);
        assertThat(merged.get("status").asText()).isEqualTo("DUPLICATE");
        JsonNode caseDetail = getJson("/api/staff/rescue-cases/" + nearCase, STAFF, 200);
        assertThat(caseDetail.get("reports").findValuesAsText("code")).contains(dupCode);
        // Báo cáo đã xử lý không còn gợi ý ca mở, cũng không gộp lại được
        assertThat(getJson("/api/staff/rescue-reports/" + dupId, STAFF, 200).get("openCases")).isEmpty();
        postJson("/api/staff/rescue-reports/" + dupId + "/merge", STAFF, map("caseId", farCase), 409);
        String history = jdbc.queryForObject(
            "SELECT event FROM status_histories WHERE entity_type = 'RESCUE_REPORT' AND entity_id = ? ORDER BY id DESC LIMIT 1",
            String.class, dupId);
        assertThat(history).isEqualTo("MERGED");
    }

    @Test
    void cannotMergeIntoClosedCase() throws Exception {
        long caseId = caseAt(10.8, 106.6);
        postJson("/api/staff/rescue-cases/" + caseId + "/fail", STAFF, map("reason", "Không tìm thấy"), 200);
        long dupId = reportId(reportAt(10.8, 106.6));
        JsonNode res = postJson("/api/staff/rescue-reports/" + dupId + "/merge", STAFF, map("caseId", caseId), 409);
        assertThat(res.get("code").asText()).isEqualTo("CASE_NOT_OPEN");
        assertThat(getJson("/api/staff/rescue-reports/" + dupId, STAFF, 200).get("status").asText()).isEqualTo("NEW");
        postJson("/api/staff/rescue-reports/" + dupId + "/merge", ADOPTER1, map("caseId", caseId), 403);
    }

    private String reportAt(double lat, double lon) throws Exception {
        return postJson("/api/public/rescue-reports", null, map("reporterName", "Người báo", "reporterPhone", randomPhone(),
            "locationText", "Có tọa độ", "latitude", lat, "longitude", lon, "description", "Chó bị thương"), 201)
            .get("code").asText();
    }

    private long caseAt(double lat, double lon) throws Exception {
        long id = reportId(reportAt(lat, lon));
        return postJson("/api/staff/rescue-reports/" + id + "/cases", STAFF,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 201).get(0).get("id").asLong();
    }
}
