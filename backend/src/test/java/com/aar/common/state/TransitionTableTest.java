package com.aar.common.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TransitionTableTest {

    @ParameterizedTest(name = "{0}: {1} --{2}--> {3}")
    @CsvSource(nullValues = "-", value = {
        "PET, -, RESCUE_SUCCEEDED, IN_CARE",
        "PET, IN_CARE, ELIGIBILITY_PASSED, AVAILABLE",
        "PET, AVAILABLE, APPLICATION_APPROVED, PENDING_HANDOVER",
        "PET, PENDING_HANDOVER, HANDED_OVER, ADOPTED",
        "PET, PENDING_HANDOVER, MARKED_INELIGIBLE, IN_CARE",
        "PET, ADOPTED, RETURNED, IN_CARE",
        "PET, LOST, FOUND, IN_CARE",
        "APPLICATION, APPROVED, CANCELLED, CANCELLED",
        "VET_REQUEST, IN_TREATMENT, COMPLETED, COMPLETED",
        "HANDOVER, IN_PROGRESS, FOLLOW_UP_FAILED, RECALL_PENDING",
        "RESCUE_REPORT, NEW, MERGED, DUPLICATE",
        "RESCUE_CASE, IN_PROGRESS, FAILED, FAILED"
    })
    void allowsTransitionsFromSection34(EntityType entity, String from, String event, String to) {
        assertThat(TransitionTable.require(entity, from, event)).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0}: {1} --{2}--> bị từ chối")
    @CsvSource({
        "PET, IN_CARE, APPLICATION_APPROVED",
        "PET, IN_TREATMENT, ELIGIBILITY_PASSED",
        "PET, DECEASED, FOUND",
        "PET, TRANSFERRED, RETURNED",
        "APPLICATION, REJECTED, APPROVED",
        "APPLICATION, HANDED_OVER, CANCELLED",
        "VET_REQUEST, COMPLETED, CANCELLED",
        "RESCUE_CASE, NEW, SUCCEEDED"
    })
    void rejectsTransitionsNotInTable(EntityType entity, String from, String event) {
        assertThatThrownBy(() -> TransitionTable.require(entity, from, event))
            .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void terminalPetStatesHaveNoWayOutExceptFound() {
        assertThat(TransitionTable.all())
            .filteredOn(t -> t.entity() == EntityType.PET)
            .filteredOn(t -> "DECEASED".equals(t.from()) || "TRANSFERRED".equals(t.from()))
            .isEmpty();
    }
}
