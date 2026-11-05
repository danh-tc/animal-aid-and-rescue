package com.aar.handover;

import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import com.aar.handover.HandoverDtos.CloseRecallRequest;
import com.aar.handover.HandoverDtos.EndEventRequest;
import com.aar.handover.HandoverDtos.EndEventResult;
import com.aar.handover.HandoverDtos.EndEventView;
import com.aar.handover.HandoverDtos.FollowUpOutcome;
import com.aar.handover.HandoverDtos.FoundRequest;
import com.aar.handover.HandoverDtos.FoundResult;
import com.aar.handover.HandoverDtos.PetCosts;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** S08 bàn giao, S09 hậu kiểm, S10 tiếp nhận trả lại. */
@RestController
@RequestMapping("/api/staff")
public class StaffHandoverController {

    private final HandoverService service;
    private final EndEventService endEvents;
    private final PetCostService costs;

    public StaffHandoverController(HandoverService service, EndEventService endEvents, PetCostService costs) {
        this.service = service;
        this.endEvents = endEvents;
        this.costs = costs;
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

    /** Mục 5.2: multipart, phần "data" là JSON kết quả, phần "photos" là ảnh hậu kiểm. */
    @PostMapping(value = "/follow-ups/{id}/result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FollowUpOutcome resultWithPhotos(@PathVariable Long id, @Valid @RequestPart("data") FollowUpResultRequest req,
                                            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
                                            @CurrentUser AuthUser me) {
        return service.recordResult(id, req, photos, me.id());
    }

    @PostMapping(value = "/follow-ups/{id}/result", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FollowUpOutcome result(@PathVariable Long id, @Valid @RequestBody FollowUpResultRequest req,
                                  @CurrentUser AuthUser me) {
        return service.recordResult(id, req, me.id());
    }

    /** QĐ-18: Quản lý đóng thu hồi khi đã quá RECALL_CLOSE_AFTER_DAYS ngày mà vẫn không thu hồi được. */
    @PostMapping("/handovers/{id}/close-recall")
    public HandoverView closeRecall(@PathVariable Long id, @Valid @RequestBody CloseRecallRequest req,
                                    @CurrentUser AuthUser me) {
        return service.closeRecall(id, req.reason(), me);
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

    /** S10: qua đời, thất lạc (QĐ-15, QĐ-16). */
    @PostMapping("/pets/{id}/end-events")
    @ResponseStatus(HttpStatus.CREATED)
    public EndEventResult endEvent(@PathVariable Long id, @Valid @RequestBody EndEventRequest req,
                                   @CurrentUser AuthUser me) {
        return endEvents.record(id, req, me.id());
    }

    @GetMapping("/pets/{id}/end-events")
    public List<EndEventView> endEvents(@PathVariable Long id) {
        return endEvents.forPet(id);
    }

    /** QĐ-16: tìm lại thú thất lạc, tạo lượt tiếp nhận FOUND. */
    @PostMapping("/pets/{id}/found")
    public FoundResult found(@PathVariable Long id, @Valid @RequestBody FoundRequest req, @CurrentUser AuthUser me) {
        return endEvents.found(id, req, me.id());
    }

    /** QĐ-14, bước 2 của UC-11: tổng chi phí điều trị, đã ủng hộ, còn chưa bù đắp. */
    @GetMapping("/pets/{id}/costs")
    public PetCosts costs(@PathVariable Long id) {
        return costs.costs(id);
    }
}
