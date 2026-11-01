package com.aar.care;

import com.aar.care.CareDtos.CareLogRequest;
import com.aar.care.CareDtos.CareLogView;
import com.aar.care.CareDtos.EligibilityRequest;
import com.aar.care.CareDtos.EligibilityResult;
import com.aar.care.CareDtos.HistoryView;
import com.aar.care.CareDtos.IneligibleResult;
import com.aar.care.CareDtos.PetDetail;
import com.aar.care.CareDtos.PetSummary;
import com.aar.care.CareDtos.ReasonRequest;
import com.aar.care.CareDtos.UpdatePetRequest;
import com.aar.care.CareDtos.VetRequestComplete;
import com.aar.care.CareDtos.VetRequestCreate;
import com.aar.care.CareDtos.VetRequestResult;
import com.aar.care.CareDtos.VetRequestView;
import com.aar.common.PageResponse;
import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import com.aar.pet.PetStatus;
import com.aar.pet.Species;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** S04, S05, S06. */
@RestController
@RequestMapping("/api/staff")
public class StaffCareController {

    private final PetCareService care;
    private final VetRequestService vet;

    public StaffCareController(PetCareService care, VetRequestService vet) {
        this.care = care;
        this.vet = vet;
    }

    @GetMapping("/pets")
    public PageResponse<PetSummary> pets(@RequestParam(required = false) PetStatus status,
                                         @RequestParam(required = false) Species species,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return care.list(status, species, q, page, size);
    }

    @GetMapping("/pets/{id}")
    public PetDetail pet(@PathVariable Long id) {
        return care.detail(id);
    }

    @PatchMapping("/pets/{id}")
    public PetSummary updatePet(@PathVariable Long id, @Valid @RequestBody UpdatePetRequest req,
                                @CurrentUser AuthUser me) {
        return care.update(id, req, me.id());
    }

    @GetMapping("/pets/{id}/history")
    public List<HistoryView> history(@PathVariable Long id) {
        return care.history(id);
    }

    @GetMapping("/pets/{id}/care-logs")
    public PageResponse<CareLogView> careLogs(@PathVariable Long id,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return care.careLogs(id, page, size);
    }

    @PostMapping("/pets/{id}/care-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public CareLogView addCareLog(@PathVariable Long id, @Valid @RequestBody CareLogRequest req,
                                  @CurrentUser AuthUser me) {
        return care.addCareLog(id, req, me.id());
    }

    @PutMapping("/pets/{id}/eligibility")
    public EligibilityResult eligibility(@PathVariable Long id, @Valid @RequestBody EligibilityRequest req,
                                         @CurrentUser AuthUser me) {
        return care.saveEligibility(id, req, me.id());
    }

    @PostMapping("/pets/{id}/ineligible")
    public IneligibleResult ineligible(@PathVariable Long id, @Valid @RequestBody ReasonRequest req,
                                       @CurrentUser AuthUser me) {
        return care.markIneligible(id, req.reason(), me.id());
    }

    @GetMapping("/pets/{id}/vet-requests")
    public List<VetRequestView> petVetRequests(@PathVariable Long id) {
        return vet.listForPet(id);
    }

    @PostMapping("/pets/{id}/vet-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public VetRequestResult createVetRequest(@PathVariable Long id, @Valid @RequestBody VetRequestCreate req,
                                             @CurrentUser AuthUser me) {
        return vet.create(id, req, me.id());
    }

    @GetMapping("/vet-requests")
    public PageResponse<VetRequestView> vetRequests(@RequestParam(required = false) VetRequestStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return vet.list(status, page, size);
    }

    @GetMapping("/vet-requests/{id}")
    public VetRequestView vetRequest(@PathVariable Long id) {
        return vet.detail(id);
    }

    @PostMapping("/vet-requests/{id}/start")
    public VetRequestResult start(@PathVariable Long id, @CurrentUser AuthUser me) {
        return vet.start(id, me.id());
    }

    @PostMapping("/vet-requests/{id}/complete")
    public VetRequestResult complete(@PathVariable Long id, @Valid @RequestBody VetRequestComplete req,
                                     @CurrentUser AuthUser me) {
        return vet.complete(id, req, me.id());
    }

    @PostMapping("/vet-requests/{id}/cancel")
    public VetRequestResult cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest req,
                                   @CurrentUser AuthUser me) {
        return vet.cancel(id, req.reason(), me.id());
    }
}
