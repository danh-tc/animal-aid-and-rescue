package com.aar.rescue;

import com.aar.common.PageResponse;
import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import com.aar.rescue.RescueDtos.AssignRequest;
import com.aar.rescue.RescueDtos.CaseDetail;
import com.aar.rescue.RescueDtos.CaseSummary;
import com.aar.rescue.RescueDtos.CreateCasesRequest;
import com.aar.rescue.RescueDtos.ReasonRequest;
import com.aar.rescue.RescueDtos.ReportDetail;
import com.aar.rescue.RescueDtos.ReportSummary;
import com.aar.rescue.RescueDtos.SucceedRequest;
import com.aar.rescue.RescueDtos.SucceedResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** S02 (báo cáo cứu hộ) và S03 (ca cứu hộ). */
@RestController
@RequestMapping("/api/staff")
public class StaffRescueController {

    private final RescueReportService reports;
    private final RescueCaseService cases;

    public StaffRescueController(RescueReportService reports, RescueCaseService cases) {
        this.reports = reports;
        this.cases = cases;
    }

    @GetMapping("/rescue-reports")
    public PageResponse<ReportSummary> listReports(@RequestParam(required = false) ReportStatus status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return reports.list(status, page, size);
    }

    @GetMapping("/rescue-reports/{id}")
    public ReportDetail reportDetail(@PathVariable Long id) {
        return reports.detail(id);
    }

    @PostMapping("/rescue-reports/{id}/cases")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CaseSummary> createCases(@PathVariable Long id, @Valid @RequestBody CreateCasesRequest req,
                                         @CurrentUser AuthUser me) {
        return reports.createCases(id, req, me.id());
    }

    @PostMapping("/rescue-reports/{id}/invalid")
    public ReportSummary markInvalid(@PathVariable Long id, @Valid @RequestBody ReasonRequest req,
                                     @CurrentUser AuthUser me) {
        return reports.markInvalid(id, req.reason(), me.id());
    }

    @GetMapping("/rescue-cases")
    public PageResponse<CaseSummary> listCases(@RequestParam(required = false) CaseStatus status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return cases.list(status, page, size);
    }

    @GetMapping("/rescue-cases/{id}")
    public CaseDetail caseDetail(@PathVariable Long id) {
        return cases.detail(id);
    }

    @PostMapping("/rescue-cases/{id}/assign")
    public CaseSummary assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req, @CurrentUser AuthUser me) {
        return cases.assign(id, req.staffId(), me.id());
    }

    @PostMapping("/rescue-cases/{id}/succeed")
    public SucceedResult succeed(@PathVariable Long id, @Valid @RequestBody SucceedRequest req,
                                 @CurrentUser AuthUser me) {
        return cases.succeed(id, req, me.id());
    }

    @PostMapping("/rescue-cases/{id}/fail")
    public CaseSummary fail(@PathVariable Long id, @Valid @RequestBody ReasonRequest req, @CurrentUser AuthUser me) {
        return cases.fail(id, req.reason(), me.id());
    }
}
