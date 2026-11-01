package com.aar.handover;

import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import com.aar.handover.HandoverDtos.FollowUpOutcome;
import com.aar.handover.HandoverDtos.FollowUpResultRequest;
import com.aar.handover.HandoverDtos.HandoverRequest;
import com.aar.handover.HandoverDtos.HandoverView;
import com.aar.handover.HandoverDtos.ReturnRequest;
import com.aar.handover.HandoverDtos.ReturnResult;
import com.aar.handover.HandoverDtos.ScheduledFollowUp;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** S08 bàn giao, S09 hậu kiểm, S10 tiếp nhận trả lại. */
@RestController
@RequestMapping("/api/staff")
public class StaffHandoverController {

    private final HandoverService service;

    public StaffHandoverController(HandoverService service) {
        this.service = service;
    }

    @PostMapping("/applications/{id}/handover")
    @ResponseStatus(HttpStatus.CREATED)
    public HandoverView handover(@PathVariable Long id, @Valid @RequestBody(required = false) HandoverRequest req,
                                 @CurrentUser AuthUser me) {
        return service.handover(id, req, me.id());
    }

    @GetMapping("/follow-ups")
    public List<ScheduledFollowUp> followUps(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore) {
        return service.scheduled(dueBefore);
    }

    @PostMapping("/follow-ups/{id}/result")
    public FollowUpOutcome result(@PathVariable Long id, @Valid @RequestBody FollowUpResultRequest req,
                                  @CurrentUser AuthUser me) {
        return service.recordResult(id, req, me.id());
    }

    @GetMapping("/pets/{id}/handovers")
    public List<HandoverView> petHandovers(@PathVariable Long id) {
        return service.forPet(id);
    }

    @PostMapping("/pets/{id}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResult returnPet(@PathVariable Long id, @Valid @RequestBody ReturnRequest req,
                                  @CurrentUser AuthUser me) {
        return service.returnPet(id, req, me.id());
    }
}
