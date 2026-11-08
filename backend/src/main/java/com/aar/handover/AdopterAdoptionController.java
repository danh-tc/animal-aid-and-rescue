package com.aar.handover;

import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import com.aar.handover.HandoverDtos.HandoverView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdopterAdoptionController {

    private final HandoverService service;

    public AdopterAdoptionController(HandoverService service) {
        this.service = service;
    }

    @GetMapping("/api/adopter/adoptions")
    public List<HandoverView> mine(@CurrentUser AuthUser me) {
        return service.forApplicant(me.id());
    }
}
