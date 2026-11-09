package com.aar.report;

import com.aar.report.ReportDtos.RecallPending;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffRecallController {

    private final ReportService service;

    public StaffRecallController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/api/staff/handovers/recall-pending")
    public List<RecallPending> recallPending() {
        return service.recallPending();
    }
}
