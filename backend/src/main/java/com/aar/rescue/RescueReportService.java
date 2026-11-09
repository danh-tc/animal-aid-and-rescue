package com.aar.rescue;

import com.aar.common.security.AuthUser;
import com.aar.rescue.RescueDtos.CaseProgress;
import com.aar.rescue.RescueDtos.PetProgress;
import com.aar.rescue.RescueDtos.PetStage;
import com.aar.pet.IntakeRepository;
import com.aar.pet.Pet;
import com.aar.pet.PetRepository;
import com.aar.rescue.RescueDtos.OpenCase;
import com.aar.rescue.RescueDtos.ReportLookup;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import com.aar.common.Pages;
import com.aar.common.PageResponse;
import com.aar.common.code.CodeGenerator;
import com.aar.common.error.BusinessException;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import com.aar.common.storage.AttachmentOwner;
import com.aar.common.storage.AttachmentRepository;
import com.aar.common.storage.AttachmentView;
import com.aar.common.storage.StorageService;
import com.aar.rescue.RescueDtos.CaseSummary;
import com.aar.rescue.RescueDtos.CreateCasesRequest;
import com.aar.rescue.RescueDtos.ReportDetail;
import com.aar.rescue.RescueDtos.ReportReceipt;
import com.aar.rescue.RescueDtos.ReportRequest;
import com.aar.rescue.RescueDtos.ReportSummary;
import com.aar.system.SystemParameterService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class RescueReportService {

    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final Set<CaseStatus> OPEN_CASES = EnumSet.of(CaseStatus.NEW, CaseStatus.IN_PROGRESS);

    private final RescueReportRepository reports;
    private final RescueCaseRepository cases;
    private final RescueCaseService caseService;
    private final StateTransitionService state;
    private final CodeGenerator codes;
    private final SystemParameterService params;
    private final StorageService storage;
    private final AttachmentRepository attachments;
    private final IntakeRepository intakes;
    private final PetRepository pets;

    public RescueReportService(RescueReportRepository reports, RescueCaseRepository cases,
                               RescueCaseService caseService, StateTransitionService state, CodeGenerator codes,
                               SystemParameterService params, StorageService storage,
                               AttachmentRepository attachments, IntakeRepository intakes, PetRepository pets) {
        this.reports = reports;
        this.cases = cases;
        this.caseService = caseService;
        this.state = state;
        this.codes = codes;
        this.params = params;
        this.storage = storage;
        this.attachments = attachments;
        this.intakes = intakes;
        this.pets = pets;
    }

    public ReportReceipt submit(ReportRequest req, List<MultipartFile> photos) {

        if ((req.latitude() == null) != (req.longitude() == null)) {
            throw BusinessException.badRequest("INVALID_COORDINATES", "Vị trí phải có đủ cả vĩ độ và kinh độ");
        }
        int limit = params.getInt(SystemParameterService.REPORT_RATE_LIMIT_PER_HOUR);
        long recent = reports.countByReporterPhoneAndCreatedAtAfter(req.reporterPhone(), OffsetDateTime.now().minusHours(1));
        if (recent >= limit) {
            throw BusinessException.tooManyRequests("RATE_LIMITED",
                "Số điện thoại này đã gửi " + limit + " báo cáo trong 1 giờ qua, vui lòng thử lại sau");
        }
        ReportStatus initial = ReportStatus.valueOf(state.initialStatus(EntityType.RESCUE_REPORT, "SUBMITTED"));
        RescueReport report = reports.save(new RescueReport(uniqueCode(), req.reporterName().trim(),
            req.reporterPhone(), req.locationText().trim(), req.latitude(), req.longitude(),
            req.description().trim(), initial));
        state.apply(EntityType.RESCUE_REPORT, report.getId(), null, "SUBMITTED", null, null);
        storage.store(AttachmentOwner.RESCUE_REPORT, report.getId(), photos);
        return new ReportReceipt(report.getCode(), report.getStatus(), report.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportSummary> list(ReportStatus status, int page, int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RescueReport> result = status == null ? reports.findAll(pageable) : reports.findByStatus(status, pageable);
        return PageResponse.of(result, ReportSummary::of);
    }

    @Transactional(readOnly = true)
    public ReportDetail detail(Long id) {
        RescueReport r = find(id);
        return new ReportDetail(r.getId(), r.getCode(), r.getReporterName(), r.getReporterPhone(),
            r.getLocationText(), r.getLatitude(), r.getLongitude(), r.getDescription(), r.getStatus(),
            r.getHandledBy(), r.getCreatedAt(), r.getHandledAt(),
            attachments.findByReportIdOrderById(id).stream().map(AttachmentView::of).toList(),
            cases.findByReportId(id).stream().map(CaseSummary::of).toList(), openCases(r));
    }

    private List<OpenCase> openCases(RescueReport report) {
        if (report.getStatus() != ReportStatus.NEW) {
            return List.of();
        }
        return cases.findByStatusInOrderByCreatedAtDesc(OPEN_CASES).stream()
            .map(c -> new OpenCase(CaseSummary.of(c), distanceKm(report, c),
                c.getReports().stream().map(RescueReport::getCode).sorted().toList()))
            .sorted(Comparator.comparing(OpenCase::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(10)
            .toList();
    }

    private static Double distanceKm(RescueReport report, RescueCase rescueCase) {
        if (report.getLatitude() == null || report.getLongitude() == null) {
            return null;
        }
        return rescueCase.getReports().stream()
            .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
            .map(r -> haversineKm(report.getLatitude().doubleValue(), report.getLongitude().doubleValue(),
                r.getLatitude().doubleValue(), r.getLongitude().doubleValue()))
            .min(Double::compare)
            .map(d -> Math.round(d * 100) / 100.0)
            .orElse(null);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public ReportSummary merge(Long reportId, Long caseId, Long by) {
        RescueReport report = find(reportId);
        RescueCase rescueCase = cases.findById(caseId)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy ca cứu hộ"));
        if (!OPEN_CASES.contains(rescueCase.getStatus())) {
            throw BusinessException.conflict("CASE_NOT_OPEN", "Chỉ gộp vào ca đang mở (Chưa phân công hoặc Đang xử lý)");
        }
        transition(report, "MERGED", by, "Gộp vào ca #" + caseId);
        rescueCase.addReport(report);
        return ReportSummary.of(report);
    }

    @Transactional(readOnly = true)
    public ReportLookup lookup(String code, String phone) {
        RescueReport r = reports.findByCodeAndReporterPhone(code.trim().toUpperCase(), phone.trim())
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy báo cáo khớp mã và số điện thoại"));
        return new ReportLookup(r.getCode(), r.getStatus(), r.getCreatedAt(), r.getHandledAt(),
            cases.findByReportId(r.getId()).stream()
                .map(c -> new CaseProgress(c.getStatus(), c.getClosedAt(), petProgress(c))).toList());
    }

    private PetProgress petProgress(RescueCase c) {
        if (c.getStatus() != CaseStatus.SUCCEEDED) return null;
        return intakes.findByRescueCaseId(c.getId())
            .flatMap(i -> pets.findById(i.getPetId()))
            .map(this::toProgress)
            .orElse(null);
    }

    private PetProgress toProgress(Pet pet) {
        PetStage stage = switch (pet.getStatus()) {
            case IN_CARE, IN_TREATMENT -> PetStage.IN_CARE;
            case AVAILABLE -> PetStage.LOOKING_FOR_HOME;
            case PENDING_HANDOVER, ADOPTED -> PetStage.ADOPTED;
            case DECEASED -> PetStage.DECEASED;
            case LOST -> PetStage.LOST;
        };
        return new PetProgress(stage, pet.getName(), stage == PetStage.LOOKING_FOR_HOME ? pet.getId() : null);
    }

    public List<CaseSummary> createCases(Long reportId, CreateCasesRequest req, AuthUser user) {
        if (req.assignedStaffId() != null) {
            RescueCaseService.requireManager(user.role());
        }
        Long by = user.id();
        RescueReport report = find(reportId);
        transition(report, "PROCESSED", by, req.numberOfAnimals() + " ca");
        List<CaseSummary> created = new ArrayList<>();
        for (int i = 0; i < req.numberOfAnimals(); i++) {
            RescueCase rescueCase = caseService.create(report, by);
            if (req.assignedStaffId() != null) {
                caseService.assign(rescueCase, req.assignedStaffId(), by);
            }
            created.add(CaseSummary.of(rescueCase));
        }
        return created;
    }

    public ReportSummary markInvalid(Long reportId, String reason, Long by) {
        RescueReport report = find(reportId);
        transition(report, "INVALIDATED", by, reason);
        return ReportSummary.of(report);
    }

    private void transition(RescueReport report, String event, Long by, String note) {
        String to = state.apply(EntityType.RESCUE_REPORT, report.getId(), report.getStatus().name(), event, by, note);
        report.changeStatus(ReportStatus.valueOf(to), by);
    }

    private RescueReport find(Long id) {
        return reports.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy báo cáo"));
    }

    private String uniqueCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = codes.nextReportCode();
            if (!reports.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã báo cáo không trùng");
    }
}
