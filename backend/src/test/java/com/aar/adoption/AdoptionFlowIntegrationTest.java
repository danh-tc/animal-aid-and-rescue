package com.aar.adoption;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.common.error.BusinessException;
import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** UC nộp đơn và UC-10 xét duyệt đơn (người C, báo cáo mục 2.1.4 và 4.4). */
class AdoptionFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    ApplicationService applicationService;

    @Test
    void adopterSubmitsAndSeesOwnApplications() throws Exception {
        long petId = availablePet(false);
        long appId = submitApplication(petId, ADOPTER1);
        JsonNode mine = getJson("/api/adopter/applications", ADOPTER1, 200);
        assertThat(mine.findValuesAsText("id")).contains(Long.toString(appId));
        assertThat(applicationStatus(appId)).isEqualTo("PENDING");

        // RB-02: không nộp hai đơn đang chờ cho cùng thú
        JsonNode dup = postJson("/api/adopter/applications", ADOPTER1, map("petId", petId, "housingType", "HOUSE",
            "hasPetExperience", true), 409);
        assertThat(dup.get("code").asText()).isEqualTo("DUPLICATE_APPLICATION");
        // Nhân viên không nộp đơn
        postJson("/api/adopter/applications", STAFF, map("petId", petId, "housingType", "HOUSE",
            "hasPetExperience", true), 403);
    }

    /** QĐ-08: chỉ thú Sẵn sàng nhận đơn. */
    @Test
    void cannotApplyForPetThatIsNotAvailable() throws Exception {
        JsonNode error = postJson("/api/adopter/applications", ADOPTER1, map("petId", 1, "housingType", "HOUSE",
            "hasPetExperience", true), 409);
        assertThat(error.get("code").asText()).isEqualTo("PET_NOT_AVAILABLE");
    }

    /** Checklist mục 5: thú chưa đủ tuổi triệt sản vẫn Sẵn sàng, nộp đơn không cần cam kết triệt sản. */
    @Test
    void youngUnsterilizedPetAcceptsApplicationWithoutPledge() throws Exception {
        long petId = availablePet(true);
        long appId = submitApplication(petId, ADOPTER1);
        assertThat(applicationStatus(appId)).isEqualTo("PENDING");
    }

    /** QĐ-09: duyệt một đơn thì thú Chờ bàn giao, các đơn chờ khác tự đóng. */
    @Test
    void approvingClosesOtherPendingApplications() throws Exception {
        long petId = availablePet(false);
        long app1 = submitApplication(petId, ADOPTER1);
        long app2 = submitApplication(petId, ADOPTER2);

        JsonNode detail = getJson("/api/staff/applications/" + app1, STAFF, 200);
        assertThat(detail.get("otherApplications")).hasSize(1);

        JsonNode approved = postJson("/api/staff/applications/" + app1 + "/approve", STAFF,
            map("note", "Điều kiện tốt"), 200);
        assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
        assertThat(petStatus(petId)).isEqualTo("PENDING_HANDOVER");
        assertThat(applicationStatus(app2)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM adoption_applications WHERE id = ?", String.class, app2))
            .contains("đơn khác được duyệt");

        JsonNode again = postJson("/api/staff/applications/" + app2 + "/approve", STAFF, null, 409);
        assertThat(again.get("code").asText()).isEqualTo("PET_ALREADY_APPROVED");
    }

    @Test
    void rejectRequiresNote() throws Exception {
        long petId = availablePet(false);
        long app = submitApplication(petId, ADOPTER3);
        postJson("/api/staff/applications/" + app + "/reject", STAFF, map("note", ""), 400);
        JsonNode rejected = postJson("/api/staff/applications/" + app + "/reject", STAFF,
            map("note", "Chung cư không cho nuôi chó"), 200);
        assertThat(rejected.get("status").asText()).isEqualTo("REJECTED");
        assertThat(petStatus(petId)).isEqualTo("AVAILABLE");
    }

    /** QĐ-11: hủy đơn đã duyệt trước bàn giao => thú về Sẵn sàng, đơn cũ đã đóng không mở lại. */
    @Test
    void adopterCancelsApprovedApplicationBeforeHandover() throws Exception {
        long petId = availablePet(false);
        long app1 = submitApplication(petId, ADOPTER1);
        long app2 = submitApplication(petId, ADOPTER2);
        postJson("/api/staff/applications/" + app1 + "/approve", STAFF, null, 200);

        // Người khác không hủy được đơn của mình
        postJson("/api/adopter/applications/" + app1 + "/cancel", ADOPTER2, map("reason", "x"), 404);
        JsonNode cancelled = postJson("/api/adopter/applications/" + app1 + "/cancel", ADOPTER1,
            map("reason", "Chuyển nhà"), 200);
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(petStatus(petId)).isEqualTo("AVAILABLE");
        assertThat(applicationStatus(app2)).isEqualTo("CANCELLED");
        postJson("/api/adopter/applications/" + app1 + "/cancel", ADOPTER1, null, 409);
    }

    /**
     * RB-01, RB-14, NFR-02: hai nhân viên duyệt hai đơn của cùng một thú cùng lúc.
     * Chỉ một thao tác thành công, thao tác còn lại nhận PET_ALREADY_APPROVED.
     */
    @Test
    void concurrentApprovalsLetOnlyOneSucceed() throws Exception {
        for (int round = 0; round < 3; round++) {
            long petId = availablePet(false);
            long app1 = submitApplication(petId, ADOPTER1);
            long app2 = submitApplication(petId, ADOPTER2);

            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            List<String> outcomes = Collections.synchronizedList(new ArrayList<>());
            List<Callable<Void>> tasks = List.of(approveTask(app1, 2L, start, outcomes),
                approveTask(app2, 3L, start, outcomes));
            List<Future<Void>> futures = new ArrayList<>();
            tasks.forEach(t -> futures.add(pool.submit(t)));
            start.countDown();
            for (Future<Void> f : futures) {
                f.get();
            }
            pool.shutdown();

            assertThat(outcomes).containsExactlyInAnyOrder("OK", "PET_ALREADY_APPROVED");
            Integer approved = jdbc.queryForObject(
                "SELECT count(*) FROM adoption_applications WHERE pet_id = ? AND status = 'APPROVED'", Integer.class, petId);
            assertThat(approved).isEqualTo(1);
            assertThat(petStatus(petId)).isEqualTo("PENDING_HANDOVER");
        }
    }

    private Callable<Void> approveTask(long appId, long staffId, CountDownLatch start, List<String> outcomes) {
        return () -> {
            start.await();
            try {
                applicationService.approve(appId, null, staffId);
                outcomes.add("OK");
            } catch (BusinessException e) {
                outcomes.add(e.code());
            }
            return null;
        };
    }
}
