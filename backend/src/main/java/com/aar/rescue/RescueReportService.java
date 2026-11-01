package com.aar.rescue;

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

/** UC báo cứu hộ (P03) và xử lý báo cáo (S02). */
@Service
@Transactional
public class RescueReportService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final RescueReportRepository reports;
    private final RescueCaseRepository cases;
    private final RescueCaseService caseService;
    private final StateTransitionService state;
    private final CodeGenerator codes;
    private final SystemParameterService params;
    private final StorageService storage;
    private final AttachmentRepository attachments;

    public RescueReportService(RescueReportRepository reports, RescueCaseRepository cases,
                               RescueCaseService caseService, StateTransitionService state, CodeGenerator codes,
                               SystemParameterService params, StorageService storage,
                               AttachmentRepository attachments) {
        this.reports = reports;
        this.cases = cases;
        this.caseService = caseService;
        this.state = state;
        this.codes = codes;
        this.params = params;
        this.storage = storage;
        this.attachments = attachments;
    }

    public ReportReceipt submit(ReportRequest req, List<MultipartFile> photos) {
        int limit = params.getInt(SystemParameterService.REPORT_RATE_LIMIT_PER_HOUR);
        long recent = reports.countByReporterPhoneAndCreatedAtAfter(req.reporterPhone(), OffsetDateTime.now().minusHours(1));
        if (recent >= limit) {
            throw BusinessException.tooManyRequests("RATE_LIMITED",
                "Số điện thoại này đã gửi " + limit + " báo cáo trong một giờ, vui lòng thử lại sau");
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
            cases.findByReportId(id).stream().map(CaseSummary::of).toList());
    }

    /** Bước 3 của UC-07: tạo một hoặc nhiều ca, mỗi ca một thú. Báo cáo chuyển sang Đã xử lý. */
    public List<CaseSummary> createCases(Long reportId, CreateCasesRequest req, Long by) {
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

    /** Ngoại lệ 2b của UC-07: báo cáo sai hoặc spam. */
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

    /** Mã ngẫu nhiên 31^6 khả năng, gần như không trùng, vẫn kiểm tra để chắc chắn. */
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
