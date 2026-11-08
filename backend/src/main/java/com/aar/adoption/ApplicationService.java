package com.aar.adoption;

import com.aar.adoption.AdoptionDtos.ApplicantBrief;
import com.aar.adoption.AdoptionDtos.ApplicantFlag;
import com.aar.adoption.AdoptionDtos.ApplicationDetail;
import com.aar.adoption.AdoptionDtos.ApplicationView;
import com.aar.adoption.AdoptionDtos.PetBrief;
import com.aar.adoption.AdoptionDtos.SubmitRequest;
import com.aar.common.Pages;
import com.aar.common.PageResponse;
import com.aar.common.error.BusinessException;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import com.aar.pet.Pet;
import com.aar.pet.PetLifecycle;
import com.aar.pet.PetRepository;
import com.aar.pet.PetStatus;
import com.aar.system.SystemParameterService;
import com.aar.system.User;
import com.aar.system.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApplicationService {

    static final String AUTO_CLOSED_REASON = "Hệ thống tự đóng: thú đã có đơn khác được duyệt";

    private final AdoptionApplicationRepository applications;
    private final PetLifecycle petLifecycle;
    private final PetRepository pets;
    private final UserRepository users;
    private final StateTransitionService state;
    private final ApplicationCascade cascade;
    private final SystemParameterService params;
    private final JdbcTemplate jdbc;

    public ApplicationService(AdoptionApplicationRepository applications, PetLifecycle petLifecycle,
                              PetRepository pets, UserRepository users, StateTransitionService state,
                              ApplicationCascade cascade, SystemParameterService params, JdbcTemplate jdbc) {
        this.params = params;
        this.jdbc = jdbc;
        this.applications = applications;
        this.petLifecycle = petLifecycle;
        this.pets = pets;
        this.users = users;
        this.state = state;
        this.cascade = cascade;
    }

    public ApplicationView submit(Long applicantId, SubmitRequest req) {
        Pet pet = petLifecycle.lock(req.petId());
        if (pet.getStatus() != PetStatus.AVAILABLE) {
            throw BusinessException.conflict("PET_NOT_AVAILABLE", "Thú không còn mở nhận nuôi");
        }
        if (applications.existsByPetIdAndApplicantIdAndStatus(pet.getId(), applicantId, ApplicationStatus.PENDING)) {
            throw BusinessException.conflict("DUPLICATE_APPLICATION", "Bạn đã có đơn đang chờ cho thú này");
        }
        int maxPending = params.getInt(SystemParameterService.MAX_PENDING_APPLICATIONS);
        if (applications.countByApplicantIdAndStatus(applicantId, ApplicationStatus.PENDING) >= maxPending) {
            throw BusinessException.conflict("TOO_MANY_PENDING",
                "Bạn đã có " + maxPending + " đơn đang chờ xét duyệt. Hãy chờ kết quả hoặc hủy bớt đơn trước khi nộp thêm");
        }

        User applicant = users.findById(applicantId).orElseThrow(this::notFound);
        String phone = blankToNull(req.phone()) != null ? req.phone().trim() : blankToNull(applicant.getPhone());
        String address = blankToNull(req.address()) != null ? req.address().trim() : blankToNull(applicant.getAddress());
        if (phone == null || address == null) {
            throw BusinessException.badRequest("CONTACT_REQUIRED",
                "Cần số điện thoại và địa chỉ để nhân viên liên hệ khi xét duyệt và hậu kiểm");
        }
        if (!phone.equals(applicant.getPhone()) || !address.equals(applicant.getAddress())) {
            applicant.updateProfile(applicant.getFullName(), phone, address);
        }
        ApplicationStatus initial = ApplicationStatus.valueOf(state.initialStatus(EntityType.APPLICATION, "SUBMITTED"));
        AdoptionApplication app = applications.save(new AdoptionApplication(pet.getId(), applicantId,
            req.housingType(), req.hasPetExperience(), blankToNull(req.motivation()), initial));
        state.apply(EntityType.APPLICATION, app.getId(), null, "SUBMITTED", applicantId, null);
        return view(app, pet, users.findById(applicantId).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ApplicationView> mine(Long applicantId) {
        return views(applications.findByApplicantIdOrderBySubmittedAtDesc(applicantId));
    }

    public ApplicationView cancelMine(Long applicationId, Long applicantId, String reason) {
        Long petId = applications.findPetIdById(applicationId).orElseThrow(this::notFound);
        Pet pet = petLifecycle.lock(petId);
        AdoptionApplication app = applications.findById(applicationId)
            .filter(a -> a.getApplicantId().equals(applicantId))
            .orElseThrow(this::notFound);
        String note = reason == null || reason.isBlank() ? "Người nhận nuôi hủy đơn" : reason.trim();
        ApplicationStatus before = app.getStatus();
        if (!ApplicationCascade.OPEN.contains(before)) {
            throw BusinessException.conflict("INVALID_TRANSITION", "Đơn đã kết thúc, không hủy được");
        }
        cascade.cancel(app, note, applicantId);
        if (before == ApplicationStatus.APPROVED && pet.getStatus() == PetStatus.PENDING_HANDOVER) {
            petLifecycle.transition(pet, "HANDOVER_CANCELLED", applicantId, "Người nhận nuôi hủy trước bàn giao");
        }
        return view(app, pet, users.findById(applicantId).orElse(null));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationView> list(ApplicationStatus status, Long petId, int page, int size) {
        var result = applications.search(status, petId, Pages.of(page, size, Sort.by("submittedAt")));
        List<ApplicationView> mapped = views(result.getContent());
        return new PageResponse<>(mapped, result.getNumber(), result.getSize(), result.getTotalElements(),
            result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ApplicationDetail detail(Long id) {
        AdoptionApplication app = applications.findById(id).orElseThrow(this::notFound);
        Pet pet = pets.findById(app.getPetId()).orElseThrow();
        List<AdoptionApplication> others = applications.findByPetIdOrderBySubmittedAtDesc(app.getPetId()).stream()
            .filter(a -> !a.getId().equals(id)).toList();
        return new ApplicationDetail(view(app, pet, users.findById(app.getApplicantId()).orElse(null)),
            pet.getStatus().name(), views(others), applicantHistory(app.getApplicantId()));
    }

    private List<ApplicantFlag> applicantHistory(Long applicantId) {
        return jdbc.query("""
            SELECT p.id pet_id, p.code, p.name, h.handed_over_at,
                   EXISTS (SELECT 1 FROM follow_ups f WHERE f.handover_id = h.id AND f.result = 'FAIL') failed,
                   (SELECT i.reason FROM intakes i WHERE i.returned_handover_id = h.id) return_reason,
                   h.follow_up_status
            FROM handovers h
            JOIN adoption_applications a ON a.id = h.application_id
            JOIN pets p ON p.id = h.pet_id
            WHERE a.applicant_id = ?
            ORDER BY h.handed_over_at DESC""", (rs, i) -> {
                boolean failed = rs.getBoolean("failed");
                String returnReason = rs.getString("return_reason");
                if (!failed && returnReason == null) {
                    return null;
                }
                String note = failed ? "Hậu kiểm Không đạt" + recallNote(rs.getString("follow_up_status"))
                    : "Đã trả lại thú: " + returnReason;
                return new ApplicantFlag(failed ? "RECALLED" : "RETURNED", rs.getLong("pet_id"), rs.getString("code"),
                    rs.getString("name"), rs.getObject("handed_over_at", OffsetDateTime.class), note);
            }, applicantId).stream().filter(java.util.Objects::nonNull).toList();
    }

    private static String recallNote(String handoverStatus) {
        return switch (handoverStatus) {
            case "RETURNED" -> ", thú đã được thu hồi";
            case "CLOSED" -> ", không thu hồi được, Quản lý đã đóng thu hồi";
            default -> ", đang chờ thu hồi";
        };
    }

    public ApplicationView cancelByStaff(Long applicationId, String reason, Long by) {
        Long petId = applications.findPetIdById(applicationId).orElseThrow(this::notFound);
        Pet pet = petLifecycle.lock(petId);
        AdoptionApplication app = applications.findById(applicationId).orElseThrow(this::notFound);
        if (app.getStatus() != ApplicationStatus.APPROVED) {
            throw BusinessException.conflict("APPLICATION_NOT_APPROVED",
                "Nhân viên chỉ hủy đơn đã duyệt khi người nhận không đến bàn giao");
        }
        cascade.cancel(app, reason.trim(), by);
        if (pet.getStatus() == PetStatus.PENDING_HANDOVER) {
            petLifecycle.transition(pet, "HANDOVER_CANCELLED", by, "Người nhận không đến bàn giao: " + reason.trim());
        }
        return view(app, pet, users.findById(app.getApplicantId()).orElse(null));
    }

    public ApplicationView approve(Long id, String note, Long by) {
        return approve(id, note, null, by);
    }

    public ApplicationView approve(Long id, String note, LocalDate handoverDate, Long by) {
        Long petId = applications.findPetIdById(id).orElseThrow(this::notFound);
        Pet pet = petLifecycle.lock(petId);
        AdoptionApplication app = applications.findById(id).orElseThrow(this::notFound);

        if (pet.getStatus() == PetStatus.PENDING_HANDOVER || pet.getStatus() == PetStatus.ADOPTED) {
            throw alreadyApproved();
        }
        if (pet.getStatus() != PetStatus.AVAILABLE) {
            throw BusinessException.conflict("PET_NOT_AVAILABLE", "Thú không còn ở trạng thái Sẵn sàng");
        }
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw BusinessException.conflict("APPLICATION_NOT_PENDING",
                "Đơn không còn ở trạng thái Chờ xét duyệt");
        }
        transition(app, "APPROVED", by, blankToNull(note));
        app.recordReview(blankToNull(note), by);
        app.scheduleHandover(handoverDate);
        try {
            applications.saveAndFlush(app);
        } catch (DataIntegrityViolationException e) {
            throw alreadyApproved();
        }
        petLifecycle.transition(pet, "APPLICATION_APPROVED", by, "Đơn #" + id);
        cascade.cancelOpen(petId, EnumSet.of(ApplicationStatus.PENDING), AUTO_CLOSED_REASON, by);
        return view(app, pet, users.findById(app.getApplicantId()).orElse(null));
    }

    public ApplicationView scheduleHandover(Long id, LocalDate date) {
        AdoptionApplication app = applications.findById(id).orElseThrow(this::notFound);
        if (app.getStatus() != ApplicationStatus.APPROVED) {
            throw BusinessException.conflict("APPLICATION_NOT_APPROVED", "Chỉ hẹn lịch bàn giao cho đơn đã duyệt");
        }
        app.scheduleHandover(date);
        return view(app, pets.findById(app.getPetId()).orElseThrow(), users.findById(app.getApplicantId()).orElse(null));
    }

    public ApplicationView reject(Long id, String note, Long by) {
        AdoptionApplication app = applications.findById(id).orElseThrow(this::notFound);
        app.recordReview(note.trim(), by);
        transition(app, "REJECTED", by, note);
        return view(app, pets.findById(app.getPetId()).orElseThrow(), users.findById(app.getApplicantId()).orElse(null));
    }

    private void transition(AdoptionApplication app, String event, Long by, String note) {
        String to = state.apply(EntityType.APPLICATION, app.getId(), app.getStatus().name(), event, by, note);
        app.changeStatus(ApplicationStatus.valueOf(to));
    }

    private List<ApplicationView> views(List<AdoptionApplication> list) {
        Set<Long> petIds = list.stream().map(AdoptionApplication::getPetId).collect(Collectors.toSet());
        Set<Long> userIds = list.stream().map(AdoptionApplication::getApplicantId).collect(Collectors.toSet());
        Map<Long, Pet> petMap = pets.findAllById(petIds).stream().collect(Collectors.toMap(Pet::getId, Function.identity()));
        Map<Long, User> userMap = users.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream().map(a -> view(a, petMap.get(a.getPetId()), userMap.get(a.getApplicantId()))).toList();
    }

    static ApplicationView view(AdoptionApplication a, Pet pet, User applicant) {
        return new ApplicationView(a.getId(),
            pet == null ? null : new PetBrief(pet.getId(), pet.getCode(), pet.getName()),
            applicant == null ? null : new ApplicantBrief(applicant.getId(), applicant.getFullName(),
                applicant.getEmail(), applicant.getPhone(), applicant.getAddress()),
            a.getHousingType(), a.isHasPetExperience(), a.getMotivation(), a.getStatus(),
            a.getReviewNote(), a.getReviewedAt(), a.getHandoverDate(), a.getCancelReason(), a.getSubmittedAt());
    }

    private static BusinessException alreadyApproved() {
        return BusinessException.conflict("PET_ALREADY_APPROVED", "Thú đã có đơn được duyệt");
    }

    private BusinessException notFound() {
        return BusinessException.notFound("Không tìm thấy đơn nhận nuôi");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
