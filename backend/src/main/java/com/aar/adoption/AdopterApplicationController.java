package com.aar.adoption;

import com.aar.adoption.AdoptionDtos.ApplicationView;
import com.aar.adoption.AdoptionDtos.CancelRequest;
import com.aar.adoption.AdoptionDtos.SubmitRequest;
import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A01, A02. */
@RestController
@RequestMapping("/api/adopter/applications")
public class AdopterApplicationController {

    private final ApplicationService service;

    public AdopterApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationView submit(@Valid @RequestBody SubmitRequest req, @CurrentUser AuthUser me) {
        return service.submit(me.id(), req);
    }

    @GetMapping
    public List<ApplicationView> mine(@CurrentUser AuthUser me) {
        return service.mine(me.id());
    }

    @PostMapping("/{id}/cancel")
    public ApplicationView cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest req,
                                  @CurrentUser AuthUser me) {
        return service.cancelMine(id, me.id(), req == null ? null : req.reason());
    }
}
