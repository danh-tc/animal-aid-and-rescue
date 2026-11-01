package com.aar.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** M01, mục 5.3: báo cáo tổng hợp, ngoại lệ, chi phí và ủng hộ. Chỉ Quản lý. */
class ReportIntegrationTest extends IntegrationTestBase {

    @Test
    void summaryCountsPeriodAndMonths() throws Exception {
        JsonNode before = getJson("/api/admin/reports/summary", MANAGER, 200);
        JsonNode handover = handedOver(ADOPTER2);
        postJson("/api/staff/pets/" + handover.get("petId").asLong() + "/returns", STAFF,
            map("reason", "Chuyển nhà", "priority", "STABLE"), 201);

        JsonNode after = getJson("/api/admin/reports/summary", MANAGER, 200);
        assertThat(after.get("period").get("to").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(after.get("months")).hasSize(12);
        assertThat(after.get("adoptions").asLong()).isEqualTo(before.get("adoptions").asLong() + 1);
        assertThat(after.get("returns").asLong()).isEqualTo(before.get("returns").asLong() + 1);
        assertThat(after.get("casesSucceeded").asLong()).isGreaterThan(before.get("casesSucceeded").asLong());
        JsonNode thisMonth = after.get("months").get(11);
        assertThat(thisMonth.get("month").asText()).isEqualTo(YearMonth.now().toString());
        assertThat(thisMonth.get("adoptions").asLong()).isPositive();
        assertThat(after.get("endEvents").has("DECEASED")).isTrue();

        JsonNode custom = getJson("/api/admin/reports/summary?from=2026-01-01&to=2026-03-31", MANAGER, 200);
        assertThat(custom.get("months").findValuesAsText("month")).containsExactly("2026-01", "2026-02", "2026-03");
        getJson("/api/admin/reports/summary?from=2026-05-01&to=2026-01-01", MANAGER, 400);
    }

    @Test
    void exceptionsListOverdueFollowUpsAndStaleApplications() throws Exception {
        JsonNode handover = handedOver(ADOPTER3);
        long followUpId = handover.get("followUps").get(0).get("id").asLong();
        jdbc.update("UPDATE follow_ups SET due_date = current_date - 3 WHERE id = ?", followUpId);

        long petId = availablePet(false);
        long appId = submitApplication(petId, ADOPTER1, false);
        jdbc.update("UPDATE adoption_applications SET submitted_at = now() - interval '10 days' WHERE id = ?", appId);

        JsonNode report = getJson("/api/admin/reports/exceptions", MANAGER, 200);
        assertThat(report.get("applicationAlertDays").asInt()).isEqualTo(7);
        JsonNode overdue = find(report.get("overdueFollowUps"), "followUpId", followUpId);
        assertThat(overdue.get("daysOverdue").asLong()).isEqualTo(3);
        JsonNode stale = find(report.get("staleApplications"), "applicationId", appId);
        assertThat(stale.get("daysWaiting").asLong()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void costsByMonthClinicAndPet() throws Exception {
        long appId = approvedApplication(ADOPTER2);
        long petId = petIdOfApplication(appId);
        postJson("/api/staff/applications/" + appId + "/handover", STAFF, map("donationAmount", 100000), 201);

        JsonNode report = getJson("/api/admin/reports/costs", MANAGER, 200);
        JsonNode month = report.get("months").get(report.get("months").size() - 1);
        assertThat(month.get("treatmentCost").decimalValue()).isGreaterThanOrEqualTo(new java.math.BigDecimal("500000"));
        assertThat(month.get("donations").decimalValue()).isGreaterThanOrEqualTo(new java.math.BigDecimal("100000"));
        assertThat(report.get("byClinic").findValuesAsText("clinicId")).contains("1");
        JsonNode pet = find(report.get("byPet"), "petId", petId);
        assertThat(pet.get("treatmentCost").decimalValue()).isEqualByComparingTo("500000");
        assertThat(pet.get("donated").decimalValue()).isEqualByComparingTo("100000");
        assertThat(pet.get("outstanding").decimalValue()).isEqualByComparingTo("400000");
    }

    @Test
    void onlyManagerCanReadReports() throws Exception {
        getJson("/api/admin/reports/summary", STAFF, 403);
        getJson("/api/admin/reports/exceptions", ADOPTER1, 403);
        getJson("/api/admin/reports/costs", null, 401);
    }

    private static JsonNode find(JsonNode list, String field, long id) {
        for (JsonNode n : list) {
            if (n.get(field).asLong() == id) {
                return n;
            }
        }
        throw new AssertionError("Không thấy " + field + "=" + id + " trong " + list);
    }
}
