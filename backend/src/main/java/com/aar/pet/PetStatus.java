package com.aar.pet;

import java.util.EnumSet;
import java.util.Set;

public enum PetStatus {
    IN_CARE, IN_TREATMENT, AVAILABLE, PENDING_HANDOVER, ADOPTED, DECEASED, LOST, TRANSFERRED;

    /** Thú đang ở trung tâm (hoặc phòng khám), nhân viên còn chăm sóc. */
    public static final Set<PetStatus> AT_CENTER = EnumSet.of(IN_CARE, IN_TREATMENT, AVAILABLE, PENDING_HANDOVER);

    public boolean atCenter() {
        return AT_CENTER.contains(this);
    }
}
