package com.aar.system;

import com.aar.system.AdminDtos.ClinicView;
import com.aar.system.AdminDtos.StaffMember;
import java.util.EnumSet;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
@Transactional(readOnly = true)
public class StaffLookupController {

    private final UserRepository users;
    private final VetClinicRepository clinics;

    public StaffLookupController(UserRepository users, VetClinicRepository clinics) {
        this.users = users;
        this.clinics = clinics;
    }

    @GetMapping("/staff-members")
    public List<StaffMember> staffMembers() {
        return users.findByRoleInAndActiveTrueOrderByFullName(EnumSet.of(Role.STAFF, Role.MANAGER)).stream()
            .map(u -> new StaffMember(u.getId(), u.getFullName(), u.getRole())).toList();
    }

    @GetMapping("/vet-clinics")
    public List<ClinicView> activeClinics() {
        return clinics.findByActiveTrueOrderByName().stream().map(ClinicView::of).toList();
    }
}
