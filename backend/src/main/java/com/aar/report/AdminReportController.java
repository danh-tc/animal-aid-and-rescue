package com.aar.report;

import com.aar.report.ReportDtos.CostReport;
import com.aar.report.ReportDtos.ExceptionReport;
import com.aar.report.ReportDtos.SummaryReport;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService service;

    public AdminReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public SummaryReport summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(ReportService.period(from, to));
    }

    @GetMapping("/exceptions")
    public ExceptionReport exceptions() {
        return service.exceptions();
    }

    @GetMapping("/costs")
    public CostReport costs(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.costs(ReportService.period(from, to));
    }
}
