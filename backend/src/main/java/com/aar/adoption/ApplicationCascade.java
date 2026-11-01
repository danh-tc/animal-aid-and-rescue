package com.aar.adoption;

import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hủy hàng loạt các đơn đang mở của một thú khi trạng thái thú thay đổi (QĐ-09, QĐ-10, QĐ-15).
 * Người gọi phải đang giữ khóa dòng thú.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ApplicationCascade {

    public static final Set<ApplicationStatus> OPEN = EnumSet.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);

    private final AdoptionApplicationRepository applications;
    private final StateTransitionService state;

    public ApplicationCascade(AdoptionApplicationRepository applications, StateTransitionService state) {
        this.applications = applications;
        this.state = state;
    }

    /** Trả về số đơn đã hủy. */
    public int cancelOpen(Long petId, Set<ApplicationStatus> statuses, String reason, Long by) {
        List<AdoptionApplication> open = applications.findByPetIdAndStatusIn(petId, statuses);
        open.forEach(app -> cancel(app, reason, by));
        return open.size();
    }

    public void cancel(AdoptionApplication app, String reason, Long by) {
        String to = state.apply(EntityType.APPLICATION, app.getId(), app.getStatus().name(), "CANCELLED", by, reason);
        app.changeStatus(ApplicationStatus.valueOf(to));
        app.recordCancel(reason);
    }
}
