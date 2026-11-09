package com.aar.handover;

import static org.assertj.core.api.Assertions.assertThat;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class DevSeedIntegrationTest extends IntegrationTestBase {

    @Test
    void seededRecallScenarioIncludesSecondFailedFollowUpAndCompleteHistory() throws Exception {
        assertThat(jdbc.queryForObject("""
            SELECT count(*)
            FROM pets p
            WHERE NOT EXISTS (
              SELECT 1 FROM status_histories h
              WHERE h.entity_type = 'PET' AND h.entity_id = p.id
            )
            """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
            SELECT count(*)
            FROM handovers ho
            WHERE NOT EXISTS (
              SELECT 1 FROM status_histories h
              WHERE h.entity_type = 'HANDOVER' AND h.entity_id = ho.id AND h.event = 'HANDED_OVER'
            )
            """, Integer.class)).isZero();

        JsonNode handovers = getJson("/api/staff/pets/9/handovers", STAFF, 200);
        assertThat(handovers).hasSize(1);
        JsonNode handover = handovers.get(0);
        assertThat(handover.get("status").asText()).isEqualTo("RECALL_PENDING");
        assertThat(handover.get("followUps").findValuesAsText("result"))
            .containsExactly("PASS", "FAIL");

        JsonNode petHistory = getJson("/api/staff/pets/9/history", STAFF, 200);
        assertThat(petHistory.findValuesAsText("event")).containsExactly(
            "RESCUE_SUCCEEDED",
            "VET_REQUEST_OPENED",
            "VET_REQUEST_CLOSED",
            "ELIGIBILITY_PASSED",
            "APPLICATION_APPROVED",
            "HANDED_OVER"
        );

        assertThat(jdbc.queryForList("""
            SELECT event
            FROM status_histories
            WHERE entity_type = 'HANDOVER' AND entity_id = 3
            ORDER BY changed_at, id
            """, String.class)).containsExactly("HANDED_OVER", "FOLLOW_UP_FAILED");
    }
}
