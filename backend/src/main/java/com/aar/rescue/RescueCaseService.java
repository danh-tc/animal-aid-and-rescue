package com.aar.rescue;

import com.aar.common.PageResponse;
import com.aar.common.Pages;
import com.aar.common.error.BusinessException;
import com.aar.common.security.AuthUser;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import com.aar.common.storage.AttachmentOwner;
import com.aar.common.storage.StorageService;
import com.aar.pet.Intake;
import com.aar.pet.IntakeRepository;
import com.aar.pet.Pet;
import com.aar.pet.PetArea;
import com.aar.pet.PetLifecycle;
import com.aar.pet.PetRepository;
import com.aar.pet.PetStatus;
import com.aar.pet.Priority;
import com.aar.rescue.RescueDtos.CaseDetail;
import com.aar.rescue.RescueDtos.CaseSummary;
import com.aar.rescue.RescueDtos.ReportSummary;
import com.aar.rescue.RescueDtos.SucceedRequest;
import com.aar.rescue.RescueDtos.SucceedResult;
import com.aar.system.Role;
import com.aar.system.User;
import com.aar.system.UserRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class RescueCaseService {

    private final RescueCaseRepository cases;
    private final StateTransitionService state;
    private final UserRepository users;
    private final PetLifecycle petLifecycle;
    private final PetRepository pets;
    private final IntakeRepository intakes;
    private final StorageService storage;

    public RescueCaseService(RescueCaseRepository cases, StateTransitionService state, UserRepository users,
                             PetLifecycle petLifecycle, PetRepository pets, IntakeRepository intakes,
                             StorageService storage) {
        this.storage = storage;
        this.cases = cases;
        this.state = state;
        this.users = users;
        this.petLifecycle = petLifecycle;
        this.pets = pets;
        this.intakes = intakes;
    }

    RescueCase create(RescueReport report, Long by) {
        CaseStatus initial = CaseStatus.valueOf(state.initialStatus(EntityType.RESCUE_CASE, "CREATED"));
        RescueCase rescueCase = cases.save(new RescueCase(report, initial, by));
        state.apply(EntityType.RESCUE_CASE, rescueCase.getId(), null, "CREATED", by, "Báo cáo " + report.getCode());
        return rescueCase;
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseSummary> list(CaseStatus status, int page, int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RescueCase> result = status == null ? cases.findAll(pageable) : cases.findByStatus(status, pageable);
        return PageResponse.of(result, CaseSummary::of);
    }

    @Transactional(readOnly = true)
    public CaseDetail detail(Long id) {
        RescueCase c = find(id);
        Intake intake = intakes.findByRescueCaseId(id).orElse(null);
        Pet pet = intake == null ? null : pets.findById(intake.getPetId()).orElse(null);
        return new CaseDetail(c.getId(), c.getLocationText(), c.getStatus(), c.getAssignedStaffId(),
            c.getFailureReason(), c.getCreatedBy(), c.getCreatedAt(), c.getClosedAt(),
            c.getReports().stream().map(ReportSummary::of).toList(),
            pet == null ? null : pet.getId(), pet == null ? null : pet.getCode());
    }

    public static void requireManager(Role role) {
        if (role != Role.MANAGER) {
            throw BusinessException.forbidden("ASSIGN_MANAGER_ONLY", "Chỉ Quản lý được phân công ca cứu hộ");
        }
    }

    public CaseSummary assign(Long caseId, Long staffId, AuthUser by) {
        requireManager(by.role());
        RescueCase rescueCase = find(caseId);
        assign(rescueCase, staffId, by.id());
        return CaseSummary.of(rescueCase);
    }

    /** Assigns a new case or reassigns an in-progress case to a different staff member. */
    void assign(RescueCase rescueCase, Long staffId, Long by) {
        User staff = users.findById(staffId)
            .filter(u -> u.isActive() && u.isStaffOrManager())
            .orElseThrow(() -> BusinessException.badRequest("INVALID_STAFF", "Người được phân công phải là nhân viên đang hoạt động"));
        boolean reassign = rescueCase.getStatus() == CaseStatus.IN_PROGRESS;
        if (reassign && staffId.equals(rescueCase.getAssignedStaffId())) {
            throw BusinessException.badRequest("SAME_ASSIGNEE", "Ca này đang do người này phụ trách");
        }
        transition(rescueCase, reassign ? "REASSIGNED" : "ASSIGNED", by,
            (reassign ? "Phân công lại cho " : "Phân công ") + staff.getFullName());
        rescueCase.assignTo(staffId);
    }

    public SucceedResult succeed(Long caseId, SucceedRequest req, AuthUser by) {
        return succeed(caseId, req, List.of(), by);
    }

    public SucceedResult succeed(Long caseId, SucceedRequest req, List<MultipartFile> photos, AuthUser by) {
        RescueCase rescueCase = find(caseId);
        requireAssigneeOrManager(rescueCase, by);
        if ((req.pet() == null) == (req.lostPetId() == null)) {
            throw BusinessException.badRequest("PET_OR_LOST_PET_REQUIRED",
                "Nhập thông tin thú mới, hoặc chọn thú thất lạc của trung tâm nếu đã xác nhận đúng con");
        }
        if (req.lostPetId() != null) {
            return succeedAsFound(rescueCase, req, photos, by.id());
        }
        transition(rescueCase, "SUCCEEDED", by.id(), null);
        var p = req.pet();
        Pet pet = petLifecycle.createFromRescue(blankToNull(p.name()), p.species(), p.sex(),
            p.estimatedBirthDate(), blankToNull(p.color()), blankToNull(p.description()), by.id());
        var i = req.intake();
        storage.store(AttachmentOwner.PET, pet.getId(), photos);
        Intake intake = intakes.save(Intake.fromRescue(pet.getId(), rescueCase.getId(), blankToNull(i.reason()),
            i.priority(), blankToNull(i.initialAssessment()), by.id()));
        return new SucceedResult(CaseSummary.of(rescueCase), pet.getId(), pet.getCode(), intake.getId(),
            i.priority() == Priority.URGENT);
    }

    private SucceedResult succeedAsFound(RescueCase rescueCase, SucceedRequest req, List<MultipartFile> photos, Long by) {
        Pet pet = petLifecycle.lock(req.lostPetId());
        if (pet.getStatus() != PetStatus.LOST) {
            throw BusinessException.conflict("PET_NOT_LOST", "Chỉ ghi nhận tìm lại cho thú đang Thất lạc");
        }
        transition(rescueCase, "SUCCEEDED", by, "Tìm lại thú thất lạc " + pet.getCode());
        var i = req.intake();
        String reason = blankToNull(i.reason()) == null ? "Tìm lại qua ca cứu hộ #" + rescueCase.getId() : i.reason().trim();
        petLifecycle.transition(pet, "FOUND", by, reason);
        pet.changeArea(PetArea.QUARANTINE);
        pet.changeLocation(PetLifecycle.CENTER_LOCATION);
        storage.store(AttachmentOwner.PET, pet.getId(), photos);
        Intake intake = intakes.save(Intake.fromFound(pet.getId(), rescueCase.getId(), reason, i.priority(),
            blankToNull(i.initialAssessment()), by));
        return new SucceedResult(CaseSummary.of(rescueCase), pet.getId(), pet.getCode(), intake.getId(),
            i.priority() == Priority.URGENT);
    }

    public CaseSummary fail(Long caseId, String reason, AuthUser by) {
        RescueCase rescueCase = find(caseId);
        requireAssigneeOrManager(rescueCase, by);
        rescueCase.recordFailure(reason.trim());
        transition(rescueCase, "FAILED", by.id(), reason);
        return CaseSummary.of(rescueCase);
    }

    private static void requireAssigneeOrManager(RescueCase rescueCase, AuthUser user) {
        if (user.role() != Role.MANAGER && !user.id().equals(rescueCase.getAssignedStaffId())) {
            throw BusinessException.forbidden("CASE_ASSIGNEE_ONLY",
                "Chỉ nhân viên được phân công hoặc Quản lý được cập nhật kết quả ca cứu hộ");
        }
    }

    private void transition(RescueCase rescueCase, String event, Long by, String note) {
        String to = state.apply(EntityType.RESCUE_CASE, rescueCase.getId(), rescueCase.getStatus().name(), event, by, note);
        rescueCase.changeStatus(CaseStatus.valueOf(to));
    }

    private RescueCase find(Long id) {
        return cases.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy ca cứu hộ"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
