package com.aar.adoption;

import com.aar.adoption.AdoptionDtos.ApplicationDetail;
import com.aar.adoption.AdoptionDtos.ApplicationView;
import com.aar.adoption.AdoptionDtos.HandoverDateRequest;
import com.aar.adoption.AdoptionDtos.RejectRequest;
import com.aar.adoption.AdoptionDtos.ReviewRequest;
import com.aar.common.PageResponse;
import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** S07: xét duyệt đơn. */
@RestController
@RequestMapping("/api/staff/applications")
public class StaffApplicationController {

    private final ApplicationService service;

    public StaffApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ApplicationView> list(@RequestParam(required = false) ApplicationStatus status,
                                              @RequestParam(required = false) Long petId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.list(status, petId, page, size);
    }

    @GetMapping("/{id}")
    public ApplicationDetail detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/approve")
    public ApplicationView approve(@PathVariable Long id, @Valid @RequestBody(required = false) ReviewRequest req,
                                   @CurrentUser AuthUser me) {
        return service.approve(id, req == null ? null : req.note(), req == null ? null : req.handoverDate(), me.id());
    }

    /** Hẹn hoặc đổi ngày bàn giao cho đơn đã duyệt (mục 2.1.4 bước 3). */
    @PostMapping("/{id}/handover-date")
    public ApplicationView handoverDate(@PathVariable Long id, @Valid @RequestBody HandoverDateRequest req) {
        return service.scheduleHandover(id, req.handoverDate());
    }

    @PostMapping("/{id}/reject")
    public ApplicationView reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req,
                                  @CurrentUser AuthUser me) {
        return service.reject(id, req.note(), me.id());
    }
}
