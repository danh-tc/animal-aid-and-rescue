package com.aar.rescue;

import com.aar.common.Pages;
import com.aar.common.PageResponse;
import com.aar.common.error.BusinessException;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import com.aar.pet.Intake;
import com.aar.pet.IntakeRepository;
import com.aar.pet.Pet;
import com.aar.pet.PetLifecycle;
import com.aar.pet.PetRepository;
import com.aar.pet.Priority;
import com.aar.rescue.RescueDtos.CaseDetail;
import com.aar.rescue.RescueDtos.CaseSummary;
import com.aar.rescue.RescueDtos.ReportSummary;
import com.aar.rescue.RescueDtos.SucceedRequest;
import com.aar.rescue.RescueDtos.SucceedResult;
import com.aar.system.User;
import com.aar.system.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ca cứu hộ (S03): phân công, thành công (tạo thú + lượt tiếp nhận), thất bại. */
@Service
@Transactional
public class RescueCaseService {

    private final RescueCaseRepository cases;
    private final StateTransitionService state;
    private final UserRepository users;
    private final PetLifecycle petLifecycle;
    private final PetRepository pets;
    private final IntakeRepository intakes;

    public RescueCaseService(RescueCaseRepository cases, StateTransitionService state, UserRepository users,
                             PetLifecycle petLifecycle, PetRepository pets, IntakeRepository intakes) {
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

    public CaseSummary assign(Long caseId, Long staffId, Long by) {
        RescueCase rescueCase = find(caseId);
        assign(rescueCase, staffId, by);
        return CaseSummary.of(rescueCase);
    }

    void assign(RescueCase rescueCase, Long staffId, Long by) {
        User staff = users.findById(staffId)
            .filter(u -> u.isActive() && u.isStaffOrManager())
            .orElseThrow(() -> BusinessException.badRequest("INVALID_STAFF", "Người được phân công phải là nhân viên đang hoạt động"));
        transition(rescueCase, "ASSIGNED", by, "Phân công " + staff.getFullName());
        rescueCase.assignTo(staffId);
    }

    /** S-1 của UC-07. Ràng buộc RB-03 (mỗi ca tối đa một lượt tiếp nhận) được database bảo đảm thêm. */
    public SucceedResult succeed(Long caseId, SucceedRequest req, Long by) {
        RescueCase rescueCase = find(caseId);
        transition(rescueCase, "SUCCEEDED", by, null);
        var p = req.pet();
        Pet pet = petLifecycle.createFromRescue(blankToNull(p.name()), p.species(), p.sex(),
            p.estimatedBirthDate(), blankToNull(p.color()), blankToNull(p.description()), by);
        var i = req.intake();
        Intake intake = intakes.save(Intake.fromRescue(pet.getId(), rescueCase.getId(), blankToNull(i.reason()),
            i.priority(), blankToNull(i.initialAssessment()), by));
        return new SucceedResult(CaseSummary.of(rescueCase), pet.getId(), pet.getCode(), intake.getId(),
            i.priority() == Priority.URGENT);
    }

    /** S-2 của UC-07: không tạo hồ sơ thú (QĐ-02). */
    public CaseSummary fail(Long caseId, String reason, Long by) {
        RescueCase rescueCase = find(caseId);
        rescueCase.recordFailure(reason.trim());
        transition(rescueCase, "FAILED", by, reason);
        return CaseSummary.of(rescueCase);
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
