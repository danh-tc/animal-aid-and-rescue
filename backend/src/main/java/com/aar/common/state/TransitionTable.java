package com.aar.common.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TransitionTable {

    private static final List<Transition> ALL;
    private static final Map<String, String> INDEX = new HashMap<>();

    static {
        List<Transition> t = new ArrayList<>();
        EntityType p = EntityType.PET;
        add(t, p, null, "RESCUE_SUCCEEDED", "IN_CARE");
        add(t, p, "IN_CARE", "VET_REQUEST_OPENED", "IN_TREATMENT");
        add(t, p, "AVAILABLE", "VET_REQUEST_OPENED", "IN_TREATMENT");
        add(t, p, "PENDING_HANDOVER", "VET_REQUEST_OPENED", "IN_TREATMENT");
        add(t, p, "IN_TREATMENT", "VET_REQUEST_CLOSED", "IN_CARE");
        add(t, p, "IN_CARE", "ELIGIBILITY_PASSED", "AVAILABLE");
        add(t, p, "AVAILABLE", "MARKED_INELIGIBLE", "IN_CARE");
        add(t, p, "PENDING_HANDOVER", "MARKED_INELIGIBLE", "IN_CARE");
        add(t, p, "AVAILABLE", "APPLICATION_APPROVED", "PENDING_HANDOVER");
        add(t, p, "PENDING_HANDOVER", "HANDOVER_CANCELLED", "AVAILABLE");
        add(t, p, "PENDING_HANDOVER", "HANDED_OVER", "ADOPTED");
        add(t, p, "ADOPTED", "RETURNED", "IN_CARE");
        add(t, p, "LOST", "FOUND", "IN_CARE");
        for (String active : List.of("IN_CARE", "IN_TREATMENT", "AVAILABLE", "PENDING_HANDOVER")) {
            add(t, p, active, "DECEASED", "DECEASED");
            add(t, p, active, "LOST", "LOST");
        }

        add(t, p, "ADOPTED", "DECEASED", "DECEASED");
        add(t, p, "ADOPTED", "LOST", "LOST");

        EntityType a = EntityType.APPLICATION;
        add(t, a, null, "SUBMITTED", "PENDING");
        add(t, a, "PENDING", "APPROVED", "APPROVED");
        add(t, a, "PENDING", "REJECTED", "REJECTED");
        add(t, a, "PENDING", "CANCELLED", "CANCELLED");
        add(t, a, "APPROVED", "HANDED_OVER", "HANDED_OVER");
        add(t, a, "APPROVED", "CANCELLED", "CANCELLED");

        EntityType v = EntityType.VET_REQUEST;
        add(t, v, null, "CREATED", "PENDING");
        add(t, v, "PENDING", "STARTED", "IN_TREATMENT");
        add(t, v, "PENDING", "COMPLETED", "COMPLETED");
        add(t, v, "IN_TREATMENT", "COMPLETED", "COMPLETED");
        add(t, v, "PENDING", "CANCELLED", "CANCELLED");
        add(t, v, "IN_TREATMENT", "CANCELLED", "CANCELLED");

        EntityType h = EntityType.HANDOVER;
        add(t, h, null, "HANDED_OVER", "IN_PROGRESS");
        add(t, h, "IN_PROGRESS", "FOLLOW_UPS_PASSED", "COMPLETED");
        add(t, h, "IN_PROGRESS", "FOLLOW_UP_FAILED", "RECALL_PENDING");
        add(t, h, "IN_PROGRESS", "RETURNED", "RETURNED");
        add(t, h, "COMPLETED", "RETURNED", "RETURNED");
        add(t, h, "RECALL_PENDING", "RETURNED", "RETURNED");
        add(t, h, "IN_PROGRESS", "CLOSED", "CLOSED");
        add(t, h, "RECALL_PENDING", "CLOSED", "CLOSED");

        add(t, h, "RECALL_PENDING", "RECALL_CLOSED", "CLOSED");

        EntityType r = EntityType.RESCUE_REPORT;
        add(t, r, null, "SUBMITTED", "NEW");
        add(t, r, "NEW", "PROCESSED", "PROCESSED");
        add(t, r, "NEW", "MERGED", "DUPLICATE");
        add(t, r, "NEW", "INVALIDATED", "INVALID");

        EntityType c = EntityType.RESCUE_CASE;
        add(t, c, null, "CREATED", "NEW");
        add(t, c, "NEW", "ASSIGNED", "IN_PROGRESS");
        add(t, c, "IN_PROGRESS", "REASSIGNED", "IN_PROGRESS");
        add(t, c, "IN_PROGRESS", "SUCCEEDED", "SUCCEEDED");
        add(t, c, "IN_PROGRESS", "FAILED", "FAILED");

        ALL = Collections.unmodifiableList(t);
    }

    private TransitionTable() {
    }

    private static void add(List<Transition> list, EntityType entity, String from, String event, String to) {
        Transition tr = new Transition(entity, from, event, to);
        String key = key(entity, from, event);
        if (INDEX.putIfAbsent(key, to) != null) {
            throw new IllegalStateException("Trùng chuyển trạng thái: " + key);
        }
        list.add(tr);
    }

    private static String key(EntityType entity, String from, String event) {
        return entity + "|" + Objects.toString(from, "") + "|" + event;
    }

    public static Optional<String> target(EntityType entity, String from, String event) {
        return Optional.ofNullable(INDEX.get(key(entity, from, event)));
    }

    public static String require(EntityType entity, String from, String event) {
        return target(entity, from, event).orElseThrow(() -> new InvalidTransitionException(entity, from, event));
    }

    public static List<Transition> all() {
        return ALL;
    }
}
