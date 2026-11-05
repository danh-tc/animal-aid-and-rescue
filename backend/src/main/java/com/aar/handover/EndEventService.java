package com.aar.handover;

import com.aar.adoption.ApplicationCascade;
import com.aar.care.VetRequestService;
import com.aar.common.error.BusinessException;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import com.aar.handover.HandoverDtos.EndEventRequest;
import com.aar.handover.HandoverDtos.EndEventResult;
import com.aar.handover.HandoverDtos.EndEventView;
import com.aar.handover.HandoverDtos.FoundRequest;
import com.aar.handover.HandoverDtos.FoundResult;
import com.aar.pet.EndEventType;
import com.aar.pet.Intake;
import com.aar.pet.IntakeRepository;
import com.aar.pet.Pet;
import com.aar.pet.PetArea;
import com.aar.pet.PetEndEvent;
import com.aar.pet.PetEndEventRepository;
import com.aar.pet.PetLifecycle;
import com.aar.pet.PetStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * S10: sự kiện kết thúc vòng đời (mục 2.1.6, QĐ-15, QĐ-16) và tìm lại thú thất lạc.
 * Mọi thay đổi trạng thái đi qua StateTransitionService theo bảng 3.4.
 */
@Service
@Transactional
public class EndEventService {

    /** Thú đã nhận nuôi chỉ ghi nhận khi còn trong thời gian hậu kiểm. */
    private static final EnumSet<HandoverStatus> FOLLOW_UP_PERIOD =
        EnumSet.of(HandoverStatus.IN_PROGRESS, HandoverStatus.RECALL_PENDING);

    private final PetLifecycle petLifecycle;
    private final PetEndEventRepository endEvents;
    private final VetRequestService vetRequests;
    private final ApplicationCascade applications;
    private final HandoverRepository handovers;
    private final FollowUpRepository followUps;
    private final IntakeRepository intakes;
    private final StateTransitionService state;

    public EndEventService(PetLifecycle petLifecycle, PetEndEventRepository endEvents, VetRequestService vetRequests,
                           ApplicationCascade applications, HandoverRepository handovers, FollowUpRepository followUps,
                           IntakeRepository intakes, StateTransitionService state) {
        this.petLifecycle = petLifecycle;
        this.endEvents = endEvents;
        this.vetRequests = vetRequests;
        this.applications = applications;
        this.handovers = handovers;
        this.followUps = followUps;
        this.intakes = intakes;
        this.state = state;
    }

    public EndEventResult record(Long petId, EndEventRequest req, Long by) {
        Pet pet = petLifecycle.lock(petId);
        EndEventType type = req.eventType();
        String detail = blankToNull(req.detail());
        if (type == EndEventType.LOST && detail == null) {
            throw BusinessException.badRequest("DETAIL_REQUIRED", "Thất lạc phải ghi nơi và hoàn cảnh mất");
        }

        Optional<Handover> activeHandover = Optional.empty();
        if (pet.getStatus() == PetStatus.ADOPTED) {
            activeHandover = handovers.findFirstByPetIdAndStatusInOrderByHandedOverAtDesc(petId, FOLLOW_UP_PERIOD);
            if (activeHandover.isEmpty()) {
                throw BusinessException.conflict("FOLLOW_UP_PERIOD_ENDED",
                    "Chỉ ghi nhận khi thú còn trong thời gian hậu kiểm (QĐ-16)");
            }
        }

        String note = label(type) + (detail == null ? "" : ": " + detail);
        // Đóng việc đang mở trước khi đổi trạng thái thú (QĐ-15)
        Map<Long, BigDecimal> costs = new HashMap<>();
        if (req.incurredCosts() != null) {
            req.incurredCosts().forEach(c -> costs.put(c.vetRequestId(), c.amount()));
        }
        int vet = vetRequests.cancelAllOpen(petId, "Hệ thống tự hủy: " + note, costs, by);
        int apps = applications.cancelOpen(petId, ApplicationCascade.OPEN, "Hệ thống tự đóng: " + note, by);
        int cancelledFollowUps = 0;
        if (activeHandover.isPresent()) {
            Handover h = activeHandover.get();
            List<FollowUp> scheduled = followUps.findByHandoverIdAndStatus(h.getId(), FollowUpStatus.SCHEDULED);
            scheduled.forEach(FollowUp::cancel);
            cancelledFollowUps = scheduled.size();
            String to = state.apply(EntityType.HANDOVER, h.getId(), h.getStatus().name(), "CLOSED", by, note);
            h.changeStatus(HandoverStatus.valueOf(to));
        }

        petLifecycle.transition(pet, type.name(), by, note);
        pet.changeArea(null);
        OffsetDateTime occurredAt = req.occurredAt() == null ? OffsetDateTime.now() : req.occurredAt();
        PetEndEvent event = endEvents.save(new PetEndEvent(petId, type, occurredAt, detail, by));
        return new EndEventResult(EndEventView.of(event), pet.getStatus(), vet, apps, cancelledFollowUps,
            activeHandover.isPresent());
    }

    /** QĐ-16: LOST => IN_CARE, lượt tiếp nhận FOUND, vào khu cách ly. */
    public FoundResult found(Long petId, FoundRequest req, Long by) {
        Pet pet = petLifecycle.lock(petId);
        if (pet.getStatus() != PetStatus.LOST) {
            throw BusinessException.conflict("PET_NOT_LOST", "Chỉ ghi nhận tìm lại cho thú đang Thất lạc");
        }
        petLifecycle.transition(pet, "FOUND", by, req.reason());
        pet.changeArea(PetArea.QUARANTINE);
        pet.changeLocation(PetLifecycle.CENTER_LOCATION);
        Intake intake = intakes.save(Intake.fromFound(petId, req.reason().trim(), req.priority(),
            blankToNull(req.initialAssessment()), by));
        return new FoundResult(petId, pet.getStatus(), intake.getId());
    }

    @Transactional(readOnly = true)
    public List<EndEventView> forPet(Long petId) {
        return endEvents.findByPetIdOrderByOccurredAtDesc(petId).stream().map(EndEventView::of).toList();
    }

    private static String label(EndEventType type) {
        return switch (type) {
            case DECEASED -> "Qua đời";
            case LOST -> "Thất lạc";
        };
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
