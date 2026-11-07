package com.aar.handover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** QĐ-14 (3 con số chi phí) và bản multipart của ghi kết quả thú y, hậu kiểm theo mục 5.2. */
class CostAndAttachmentIntegrationTest extends IntegrationTestBase {

    @Test
    void costsAccumulateAcrossHandoversAndNeverNegative() throws Exception {
        long appId = approvedApplication(ADOPTER2);
        long petId = petIdOfApplication(appId);
        // availablePet đã có một lần khám 500.000
        JsonNode before = getJson("/api/staff/pets/" + petId + "/costs", STAFF, 200);
        assertThat(before.get("totalTreatmentCost").decimalValue()).isEqualByComparingTo("500000");
        assertThat(before.get("totalDonated").decimalValue()).isEqualByComparingTo("0");
        assertThat(before.get("outstanding").decimalValue()).isEqualByComparingTo("500000");

        postJson("/api/staff/applications/" + appId + "/handover", STAFF, map("donationAmount", 200000), 201);
        JsonNode after = getJson("/api/staff/pets/" + petId + "/costs", STAFF, 200);
        assertThat(after.get("totalDonated").decimalValue()).isEqualByComparingTo("200000");
        assertThat(after.get("outstanding").decimalValue()).isEqualByComparingTo("300000");

        // Trả lại rồi bàn giao lần 2 với khoản ủng hộ vượt chi phí: còn chưa bù đắp thấp nhất là 0
        postJson("/api/staff/pets/" + petId + "/returns", STAFF, map("reason", "Dị ứng lông", "priority", "STABLE"), 201);
        backdateIntakes(petId, 30);
        assertThat(putJson("/api/staff/pets/" + petId + "/eligibility", STAFF, map("vaccinated", true,
            "dewormed", true, "sterilized", true, "temperamentOk", true), 200).get("status").asText()).isEqualTo("AVAILABLE");
        long app2 = submitApplication(petId, ADOPTER3);
        postJson("/api/staff/applications/" + app2 + "/approve", STAFF, null, 200);
        postJson("/api/staff/applications/" + app2 + "/handover", STAFF, map("donationAmount", 1000000), 201);
        JsonNode second = getJson("/api/staff/pets/" + petId + "/costs", STAFF, 200);
        assertThat(second.get("totalDonated").decimalValue()).isEqualByComparingTo("1200000");
        assertThat(second.get("outstanding").decimalValue()).isEqualByComparingTo("0");

        getJson("/api/staff/pets/999999/costs", STAFF, 404);
    }

    @Test
    void vetCompleteMultipartStoresReceipt() throws Exception {
        long petId = rescuedPet("DOG", java.time.LocalDate.now().minusYears(1));
        long vetId = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", 1, "reason", "Khám", "priority", "STABLE", "careMode", "OUTPATIENT"), 201)
            .get("request").get("id").asLong();
        String data = json.writeValueAsString(map("diagnosisSummary", "Ổn", "totalCost", 120000, "healthOk", true));
        JsonNode res = call(multipart("/api/staff/vet-requests/" + vetId + "/complete")
            .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, data.getBytes()))
            .file(new MockMultipartFile("files", "phieu-kham.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{37, 80, 68, 70})),
            STAFF, 200);
        assertThat(res.get("request").get("status").asText()).isEqualTo("COMPLETED");
        assertThat(res.get("petStatus").asText()).isEqualTo("IN_CARE");
        JsonNode files = getJson("/api/staff/vet-requests/" + vetId + "/attachments", STAFF, 200);
        assertThat(files.findValuesAsText("originalName")).containsExactly("phieu-kham.pdf");
    }

    @Test
    void followUpResultMultipartStoresPhotos() throws Exception {
        JsonNode handover = handedOver(ADOPTER1);
        long followUpId = handover.get("followUps").get(0).get("id").asLong();
        String data = json.writeValueAsString(HandoverFlowIntegrationTest.criteria("PASS", null));
        JsonNode res = call(multipart("/api/staff/follow-ups/" + followUpId + "/result")
            .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, data.getBytes()))
            .file(new MockMultipartFile("photos", "nha-moi.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3})),
            STAFF, 200);
        assertThat(res.get("followUp").get("result").asText()).isEqualTo("PASS");
        assertThat(getJson("/api/staff/follow-ups/" + followUpId + "/attachments", STAFF, 200)
            .findValuesAsText("originalName")).containsExactly("nha-moi.jpg");
    }
}
