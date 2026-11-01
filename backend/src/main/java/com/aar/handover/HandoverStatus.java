package com.aar.handover;

import java.util.EnumSet;
import java.util.Set;

/** Cột handovers.follow_up_status: tiến trình hậu kiểm sau bàn giao. */
public enum HandoverStatus {
    IN_PROGRESS, COMPLETED, RECALL_PENDING, RETURNED, CLOSED;

    /** Thú còn ở với chủ nuôi, có thể bị trả lại (QĐ-13). */
    public static final Set<HandoverStatus> WITH_OWNER = EnumSet.of(IN_PROGRESS, COMPLETED, RECALL_PENDING);
}
