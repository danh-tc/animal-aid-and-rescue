package com.aar.handover;

import java.util.EnumSet;
import java.util.Set;

public enum HandoverStatus {
    IN_PROGRESS, COMPLETED, RECALL_PENDING, RETURNED, CLOSED;

    public static final Set<HandoverStatus> WITH_OWNER = EnumSet.of(IN_PROGRESS, COMPLETED, RECALL_PENDING);
}
