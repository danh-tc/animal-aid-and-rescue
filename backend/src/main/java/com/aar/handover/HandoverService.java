package com.aar.handover;

import com.aar.common.storage.AttachmentOwner;
import com.aar.common.storage.StorageService;
import org.springframework.web.multipart.MultipartFile;

import com.aar.adoption.AdoptionApplication;
import com.aar.adoption.AdoptionApplicationRepository;
import com.aar.adoption.ApplicationStatus;
import com.aar.common.error.BusinessException;
import com.aar.common.security.AuthUser;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import com.aar.handover.HandoverDtos.FollowUpOutcome;
import com.aar.handover.HandoverDtos.FollowUpResultRequest;
import com.aar.handover.HandoverDtos.FollowUpView;
import com.aar.handover.HandoverDtos.HandoverRequest;
import com.aar.handover.HandoverDtos.HandoverView;
import com.aar.handover.HandoverDtos.ReturnRequest;
import com.aar.handover.HandoverDtos.ReturnResult;
import com.aar.handover.HandoverDtos.ScheduledFollowUp;
import com.aar.pet.Intake;
import com.aar.pet.IntakeRepository;
import com.aar.pet.Pet;
import com.aar.pet.PetArea;
import com.aar.pet.PetLifecycle;
import com.aar.pet.PetRepository;
import com.aar.pet.PetStatus;
import com.aar.system.Role;
import com.aar.system.SystemParameterService;
import com.aar.system.User;
import com.aar.system.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HandoverService {

    private final HandoverRepository handovers;
    private final FollowUpRepository followUps;
    private final AdoptionApplicationRepository applications;
    private final PetLifecycle petLifecycle;
    private final PetRepository pets;
    private final UserRepository users;
    private final IntakeRepository intakes;
    private final StateTransitionService state;
    private final SystemParameterService params;
    private final StorageService storage;

    public HandoverService(HandoverRepository handovers, FollowUpRepository followUps,
                           AdoptionApplicationRepository applications, PetLifecycle petLifecycle,
                           PetRepository pets, UserRepository users, IntakeRepository intakes,
                           StateTransitionService state, SystemParameterService params, StorageService storage) {
        this.storage = storage;
        this.handovers = handovers;
        this.followUps = followUps;
        this.applications = applications;
        this.petLifecycle = petLifecycle;
        this.pets = pets;
        this.users = users;
        this.intakes = intakes;
        this.state = state;
        this.params = params;
    }

    public HandoverView handover(Long applicationId, HandoverRequest req, Long by) {
        Long petId = applications.findPetIdById(applicationId)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn nhận nuôi"));
        Pet pet = petLifecycle.lock(petId);
        AdoptionApplication app = applications.findById(applicationId).orElseThrow();
        if (app.getStatus() != ApplicationStatus.APPROVED) {
            throw BusinessException.conflict("APPLICATION_NOT_APPROVED", "Chỉ bàn giao đơn đã được duyệt");
        }
        String appTo = state.apply(EntityType.APPLICATION, app.getId(), app.getStatus().name(), "HANDED_OVER", by, null);
        app.changeStatus(ApplicationStatus.valueOf(appTo));
        petLifecycle.transition(pet, "HANDED_OVER", by, "Đơn #" + applicationId);
        pet.changeArea(null);
        pet.changeLocation(PetLifecycle.OWNER_LOCATION);

        HandoverStatus initial = HandoverStatus.valueOf(state.initialStatus(EntityType.HANDOVER, "HANDED_OVER"));
        BigDecimal donation = req == null || req.donationAmount() == null ? BigDecimal.ZERO : req.donationAmount();
        Handover handover = handovers.save(new Handover(app.getId(), pet.getId(), by, donation,
            req == null ? null : blankToNull(req.note()), initial));
        state.apply(EntityType.HANDOVER, handover.getId(), null, "HANDED_OVER", by, null);

        LocalDate today = LocalDate.now();
        followUps.save(new FollowUp(handover.getId(), 1, false,
            today.plusDays(params.getInt(SystemParameterService.FOLLOW_UP_1_DAYS))));
        followUps.save(new FollowUp(handover.getId(), 2, false,
            today.plusDays(params.getInt(SystemParameterService.FOLLOW_UP_2_DAYS))));
        return view(handover);
    }

    @Transactional(readOnly = true)
    public List<ScheduledFollowUp> scheduled(LocalDate dueBefore) {
        List<FollowUp> list = dueBefore == null
            ? followUps.findByStatusOrderByDueDateAscIdAsc(FollowUpStatus.SCHEDULED)
            : followUps.findByStatusAndDueDateLessThanEqualOrderByDueDateAscIdAsc(FollowUpStatus.SCHEDULED, dueBefore);
        Map<Long, Handover> handoverMap = byId(handovers.findAllById(ids(list, FollowUp::getHandoverId)), Handover::getId);
        Map<Long, Pet> petMap = byId(pets.findAllById(ids(handoverMap.values(), Handover::getPetId)), Pet::getId);
        Map<Long, AdoptionApplication> appMap = byId(
            applications.findAllById(ids(handoverMap.values(), Handover::getApplicationId)), AdoptionApplication::getId);
        Map<Long, User> userMap = byId(users.findAllById(ids(appMap.values(), AdoptionApplication::getApplicantId)), User::getId);
        return list.stream().map(f -> {
            Handover h = handoverMap.get(f.getHandoverId());
            Pet p = petMap.get(h.getPetId());
            User owner = userMap.get(appMap.get(h.getApplicationId()).getApplicantId());
            return new ScheduledFollowUp(FollowUpView.of(f), p.getId(), p.getCode(), p.getName(),
                owner.getFullName(), owner.getPhone(), owner.getAddress());
        }).toList();
    }

    public FollowUpOutcome recordResult(Long followUpId, FollowUpResultRequest req, Long by) {
        return recordResult(followUpId, req, List.of(), by);
    }

    public FollowUpOutcome recordResult(Long followUpId, FollowUpResultRequest req, List<MultipartFile> photos, Long by) {
        FollowUp followUp = followUps.findById(followUpId)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lịch hậu kiểm"));
        Handover handover = handovers.findById(followUp.getHandoverId()).orElseThrow();
        petLifecycle.lock(handover.getPetId());
        if (followUp.getStatus() != FollowUpStatus.SCHEDULED) {
            throw BusinessException.conflict("FOLLOW_UP_NOT_SCHEDULED", "Lần hậu kiểm này đã có kết quả hoặc đã bị hủy");
        }
        if (handover.getStatus() != HandoverStatus.IN_PROGRESS) {
            throw BusinessException.conflict("FOLLOW_UP_CLOSED", "Bàn giao không còn trong thời gian hậu kiểm");
        }
        String reason = blankToNull(req.reason());
        if (req.result() != FollowUpResult.PASS && reason == null) {
            throw BusinessException.badRequest("REASON_REQUIRED", "Kết quả Cần theo dõi hoặc Không đạt phải ghi lý do");
        }

        if (followUp.isAdditional() && req.result() == FollowUpResult.WATCH) {
            throw BusinessException.badRequest("ADDITIONAL_RESULT_INVALID",
                "Lần hậu kiểm bổ sung chỉ được chọn Đạt hoặc Không đạt");
        }
        followUp.recordResult(req.result(), req.contactMethod(), req.healthOk(), req.housingOk(), req.commitmentOk(), req.petPresent(),
            req.ownerReachable(), reason, by);
        followUps.flush();
        storage.store(AttachmentOwner.FOLLOW_UP, followUp.getId(), photos);

        FollowUp additional = null;
        switch (req.result()) {
            case WATCH -> {
                int next = followUps.findByHandoverIdOrderBySequenceNo(handover.getId()).stream()
                    .mapToInt(FollowUp::getSequenceNo).max().orElse(0) + 1;
                additional = followUps.save(new FollowUp(handover.getId(), next, true,
                    LocalDate.now().plusDays(params.getInt(SystemParameterService.FOLLOW_UP_1_DAYS))));
            }
            case FAIL -> {
                cancelScheduled(handover.getId());
                transition(handover, "FOLLOW_UP_FAILED", by, reason);
            }
            case PASS -> {
                if (followUps.findByHandoverIdAndStatus(handover.getId(), FollowUpStatus.SCHEDULED).isEmpty()) {
                    transition(handover, "FOLLOW_UPS_PASSED", by, null);
                }
            }
        }
        return new FollowUpOutcome(FollowUpView.of(followUp), handover.getStatus(),
            additional == null ? null : FollowUpView.of(additional));
    }

    public HandoverView closeRecall(Long handoverId, String reason, AuthUser by) {
        if (by.role() != Role.MANAGER) {
            throw BusinessException.forbidden("CLOSE_RECALL_MANAGER_ONLY", "Chỉ Quản lý được đóng thu hồi");
        }
        Handover handover = handovers.findById(handoverId)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bàn giao"));
        petLifecycle.lock(handover.getPetId());
        if (handover.getStatus() != HandoverStatus.RECALL_PENDING) {
            throw BusinessException.conflict("NOT_RECALL_PENDING", "Bàn giao không ở trạng thái Chờ thu hồi");
        }
        int days = params.getInt(SystemParameterService.RECALL_CLOSE_AFTER_DAYS);
        OffsetDateTime failedAt = followUps.findByHandoverIdOrderBySequenceNo(handoverId).stream()
            .filter(f -> f.getResult() == FollowUpResult.FAIL && f.getPerformedAt() != null)
            .map(FollowUp::getPerformedAt).max(OffsetDateTime::compareTo)
            .orElseThrow(() -> BusinessException.conflict("NOT_RECALL_PENDING", "Không tìm thấy lần hậu kiểm Không đạt"));
        OffsetDateTime allowedAt = failedAt.plusDays(days);
        if (OffsetDateTime.now().isBefore(allowedAt)) {
            long left = Math.max(1, java.time.Duration.between(OffsetDateTime.now(), allowedAt).toDays() + 1);
            throw BusinessException.conflict("RECALL_CLOSE_TOO_EARLY",
                "Chỉ đóng thu hồi được sau " + days + " ngày kể từ lần hậu kiểm Không đạt, còn khoảng " + left + " ngày nữa");
        }
        transition(handover, "RECALL_CLOSED", by.id(), reason.trim());
        return view(handover);
    }

    public ReturnResult returnPet(Long petId, ReturnRequest req, Long by) {
        Pet pet = petLifecycle.lock(petId);
        if (pet.getStatus() != PetStatus.ADOPTED) {
            throw BusinessException.conflict("PET_NOT_ADOPTED", "Chỉ tiếp nhận trả lại thú đang ở trạng thái Đã nhận nuôi");
        }
        Handover handover = handovers.findFirstByPetIdAndStatusInOrderByHandedOverAtDesc(petId, HandoverStatus.WITH_OWNER)
            .orElseThrow(() -> BusinessException.conflict("NO_ACTIVE_HANDOVER", "Không tìm thấy bàn giao còn hiệu lực"));
        int cancelled = cancelScheduled(handover.getId());
        transition(handover, "RETURNED", by, req.reason());
        Intake intake = intakes.save(Intake.fromReturn(petId, handover.getId(), req.reason().trim(), req.priority(),
            blankToNull(req.initialAssessment()), by));
        petLifecycle.transition(pet, "RETURNED", by, req.reason());
        pet.changeArea(PetArea.QUARANTINE);
        pet.changeLocation(PetLifecycle.CENTER_LOCATION);
        return new ReturnResult(petId, pet.getStatus().name(), intake.getId(), handover.getId(), cancelled);
    }

    @Transactional(readOnly = true)
    public List<HandoverView> forPet(Long petId) {
        return handovers.findByPetIdOrderByHandedOverAtDesc(petId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<HandoverView> forApplicant(Long applicantId) {
        return handovers.findByApplicant(applicantId).stream().map(this::view).toList();
    }

    private int cancelScheduled(Long handoverId) {
        List<FollowUp> scheduled = followUps.findByHandoverIdAndStatus(handoverId, FollowUpStatus.SCHEDULED);
        scheduled.forEach(FollowUp::cancel);
        return scheduled.size();
    }

    private void transition(Handover handover, String event, Long by, String note) {
        String to = state.apply(EntityType.HANDOVER, handover.getId(), handover.getStatus().name(), event, by, note);
        handover.changeStatus(HandoverStatus.valueOf(to));
    }

    private HandoverView view(Handover h) {
        Pet pet = pets.findById(h.getPetId()).orElseThrow();
        AdoptionApplication app = applications.findById(h.getApplicationId()).orElseThrow();
        User owner = users.findById(app.getApplicantId()).orElseThrow();
        return new HandoverView(h.getId(), h.getApplicationId(), pet.getId(), pet.getCode(), pet.getName(),
            owner.getId(), owner.getFullName(), owner.getPhone(), h.getHandedOverAt(), h.getDonationAmount(),
            h.getNote(), h.getStatus(),
            followUps.findByHandoverIdOrderBySequenceNo(h.getId()).stream().map(FollowUpView::of).toList());
    }

    private static <T> Set<Long> ids(java.util.Collection<T> items, Function<T, Long> getter) {
        return items.stream().map(getter).collect(Collectors.toSet());
    }

    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> getter) {
        return items.stream().collect(Collectors.toMap(getter, Function.identity()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
