package com.aar.care;

import com.aar.adoption.ApplicationCascade;
import com.aar.adoption.ApplicationStatus;
import com.aar.care.CareDtos.CareLogRequest;
import com.aar.care.CareDtos.CareLogView;
import com.aar.care.CareDtos.EligibilityRequest;
import com.aar.care.CareDtos.EligibilityResult;
import com.aar.care.CareDtos.HistoryView;
import com.aar.care.CareDtos.IneligibleResult;
import com.aar.care.CareDtos.IntakeView;
import com.aar.care.CareDtos.PetDetail;
import com.aar.care.CareDtos.PetSummary;
import com.aar.care.CareDtos.UpdatePetRequest;
import com.aar.care.EligibilityService.Checklist;
import com.aar.common.Pages;
import com.aar.common.PageResponse;
import com.aar.common.error.BusinessException;
import com.aar.common.state.EntityType;
import com.aar.common.state.StatusHistoryRepository;
import com.aar.common.storage.AttachmentRepository;
import com.aar.common.storage.AttachmentView;
import com.aar.pet.IntakeRepository;
import com.aar.pet.Pet;
import com.aar.pet.PetArea;
import com.aar.pet.PetLifecycle;
import com.aar.pet.PetRepository;
import com.aar.pet.PetStatus;
import com.aar.pet.Species;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** S04, S05: hồ sơ thú, nhật ký chăm sóc, checklist, đánh dấu không đủ điều kiện, lịch sử trạng thái. */
@Service
@Transactional
public class PetCareService {

    private final PetRepository pets;
    private final PetLifecycle petLifecycle;
    private final IntakeRepository intakes;
    private final CareLogRepository careLogs;
    private final EligibilityService eligibility;
    private final VetRequestRepository vetRequests;
    private final VetRequestService vetRequestService;
    private final ApplicationCascade applicationCascade;
    private final StatusHistoryRepository histories;
    private final AttachmentRepository attachments;

    public PetCareService(PetRepository pets, PetLifecycle petLifecycle, IntakeRepository intakes,
                          CareLogRepository careLogs, EligibilityService eligibility,
                          VetRequestRepository vetRequests, VetRequestService vetRequestService,
                          ApplicationCascade applicationCascade, StatusHistoryRepository histories,
                          AttachmentRepository attachments) {
        this.pets = pets;
        this.petLifecycle = petLifecycle;
        this.intakes = intakes;
        this.careLogs = careLogs;
        this.eligibility = eligibility;
        this.vetRequests = vetRequests;
        this.vetRequestService = vetRequestService;
        this.applicationCascade = applicationCascade;
        this.histories = histories;
        this.attachments = attachments;
    }

    @Transactional(readOnly = true)
    public PageResponse<PetSummary> list(PetStatus status, Species species, String q, int page, int size) {
        String pattern = q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(pets.search(status, species, pattern, pageable), PetSummary::of);
    }

    @Transactional(readOnly = true)
    public PetDetail detail(Long petId) {
        Pet p = find(petId);
        return new PetDetail(p.getId(), p.getCode(), p.getName(), p.getSpecies(), p.getSex(),
            p.getEstimatedBirthDate(), p.getColor(), p.getDescription(), p.getStatus(), p.getArea(),
            p.getCurrentLocation(), p.isVaccinated(), p.isDewormed(), p.isSterilized(), p.isTemperamentOk(),
            p.getEligibilityCheckedAt(), p.getEligibilityCheckedBy(), p.getVersion(), p.getCreatedAt(),
            intakes.findByPetIdOrderByIntakeAtDesc(petId).stream().map(IntakeView::of).toList(),
            eligibility.evaluate(p),
            vetRequestService.views(vetRequests.findByPetIdOrderByRequestedAtDesc(petId)),
            attachments.findByPetIdOrderById(petId).stream().map(AttachmentView::of).toList());
    }

    /** Sửa thông tin mô tả, khu vực, nơi ở. Đổi khu vực tự ghi nhật ký AREA_CHANGE. */
    public PetSummary update(Long petId, UpdatePetRequest req, Long by) {
        Pet pet = find(petId);
        pet.updateInfo(req.name() != null ? blankToNull(req.name()) : pet.getName(),
            req.sex() != null ? req.sex() : pet.getSex(),
            req.estimatedBirthDate() != null ? req.estimatedBirthDate() : pet.getEstimatedBirthDate(),
            req.color() != null ? blankToNull(req.color()) : pet.getColor(),
            req.description() != null ? blankToNull(req.description()) : pet.getDescription());
        if (req.area() != null && req.area() != pet.getArea()) {
            requireAtCenter(pet);
            PetArea old = pet.getArea();
            pet.changeArea(req.area());
            careLogs.save(new CareLog(petId, by, CareActivity.AREA_CHANGE, null,
                "Chuyển khu vực " + (old == null ? "-" : old) + " => " + req.area()));
        }
        if (req.currentLocation() != null) {
            pet.changeLocation(blankToNull(req.currentLocation()));
        }
        return PetSummary.of(pet);
    }

    @Transactional(readOnly = true)
    public PageResponse<CareLogView> careLogs(Long petId, int page, int size) {
        find(petId);
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "loggedAt"));
        return PageResponse.of(careLogs.findByPetId(petId, pageable), CareLogView::of);
    }

    public CareLogView addCareLog(Long petId, CareLogRequest req, Long by) {
        Pet pet = find(petId);
        requireAtCenter(pet);
        if (req.activityType() == CareActivity.WEIGHT && req.weightKg() == null) {
            throw BusinessException.badRequest("WEIGHT_REQUIRED", "Ghi cân nặng phải có số kg");
        }
        if (req.activityType() == CareActivity.AREA_CHANGE) {
            throw BusinessException.badRequest("USE_PET_UPDATE", "Đổi khu vực qua cập nhật hồ sơ thú, hệ thống tự ghi nhật ký");
        }
        return CareLogView.of(careLogs.save(new CareLog(petId, by, req.activityType(),
            req.activityType() == CareActivity.WEIGHT ? req.weightKg() : null, blankToNull(req.note()))));
    }

    /**
     * S-4 của UC-09: lưu checklist. Thú Đang chăm sóc đạt đủ 6 mục thì chuyển Sẵn sàng (QĐ-07, RB-13).
     * Thú đang Sẵn sàng hoặc Chờ bàn giao muốn hạ điều kiện thì dùng ineligible().
     */
    public EligibilityResult saveEligibility(Long petId, EligibilityRequest req, Long by) {
        Pet pet = petLifecycle.lock(petId);
        if (pet.getStatus() != PetStatus.IN_CARE && pet.getStatus() != PetStatus.IN_TREATMENT) {
            throw BusinessException.conflict("CHECKLIST_LOCKED",
                "Chỉ cập nhật checklist khi thú Đang chăm sóc hoặc Đang điều trị");
        }
        pet.updateChecklist(req.vaccinated(), req.dewormed(), req.sterilized(), req.temperamentOk());
        Checklist checklist = eligibility.evaluate(pet);
        if (checklist.allPassed() && pet.getStatus() == PetStatus.IN_CARE) {
            petLifecycle.transition(pet, "ELIGIBILITY_PASSED", by, null);
            pet.markEligibilityChecked(by);
            if (pet.getArea() == PetArea.QUARANTINE) {
                pet.changeArea(PetArea.GENERAL);
                careLogs.save(new CareLog(petId, by, CareActivity.AREA_CHANGE, null,
                    "Đạt điều kiện nhận nuôi, chuyển khu chung"));
            }
        }
        return new EligibilityResult(pet.getStatus(), checklist, checklist.missing());
    }

    /** QĐ-10: thú không đủ điều kiện khi Sẵn sàng hoặc Chờ bàn giao, hủy đơn đang chờ và đã duyệt. */
    public IneligibleResult markIneligible(Long petId, String reason, Long by) {
        Pet pet = petLifecycle.lock(petId);
        if (pet.getStatus() != PetStatus.AVAILABLE && pet.getStatus() != PetStatus.PENDING_HANDOVER) {
            throw BusinessException.conflict("INVALID_TRANSITION",
                "Chỉ đánh dấu không đủ điều kiện khi thú Sẵn sàng hoặc Chờ bàn giao");
        }
        int cancelled = applicationCascade.cancelOpen(petId,
            EnumSet.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED),
            "Hệ thống tự đóng: thú không đủ điều kiện nhận nuôi", by);
        petLifecycle.transition(pet, "MARKED_INELIGIBLE", by, reason);
        return new IneligibleResult(pet.getStatus(), cancelled);
    }

    @Transactional(readOnly = true)
    public List<HistoryView> history(Long petId) {
        find(petId);
        return histories.findByEntityTypeAndEntityIdOrderByChangedAtAsc(EntityType.PET, petId).stream()
            .map(h -> new HistoryView(h.getFromStatus(), h.getToStatus(), h.getEvent(), h.getNote(),
                h.getChangedBy(), h.getChangedAt()))
            .toList();
    }

    private void requireAtCenter(Pet pet) {
        if (!pet.getStatus().atCenter()) {
            throw BusinessException.conflict("PET_NOT_AT_CENTER", "Thú không còn ở trung tâm");
        }
    }

    private Pet find(Long id) {
        return pets.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy thú"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
