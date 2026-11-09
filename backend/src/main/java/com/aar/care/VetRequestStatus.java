package com.aar.care;

import java.util.EnumSet;
import java.util.Set;

public enum VetRequestStatus {
    PENDING, IN_TREATMENT, COMPLETED, CANCELLED;

    public static final Set<VetRequestStatus> OPEN = EnumSet.of(PENDING, IN_TREATMENT);
}
