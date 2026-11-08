package com.aar.system;

import com.aar.common.PageResponse;
import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import com.aar.system.AdminDtos.ClinicRequest;
import com.aar.system.AdminDtos.ClinicView;
import com.aar.system.AdminDtos.CreateUserRequest;
import com.aar.system.AdminDtos.ParameterRequest;
import com.aar.system.AdminDtos.ParameterView;
import com.aar.system.AdminDtos.UpdateUserRequest;
import com.aar.system.AdminDtos.UserAdminView;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final SystemParameterService params;

    public AdminController(AdminService admin, SystemParameterService params) {
        this.admin = admin;
        this.params = params;
    }

    @GetMapping("/users")
    public PageResponse<UserAdminView> users(@RequestParam(required = false) Role role,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return admin.listUsers(role, page, size);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserAdminView createUser(@Valid @RequestBody CreateUserRequest req) {
        return admin.createUser(req);
    }

    @PatchMapping("/users/{id}")
    public UserAdminView updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req,
                                    @CurrentUser AuthUser me) {
        return admin.updateUser(id, req, me.id());
    }

    @GetMapping("/vet-clinics")
    public List<ClinicView> clinics() {
        return admin.listClinics();
    }

    @PostMapping("/vet-clinics")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicView createClinic(@Valid @RequestBody ClinicRequest req) {
        return admin.createClinic(req);
    }

    @PatchMapping("/vet-clinics/{id}")
    public ClinicView updateClinic(@PathVariable Long id, @Valid @RequestBody ClinicRequest req) {
        return admin.updateClinic(id, req);
    }

    @GetMapping("/system-parameters")
    public List<ParameterView> parameters() {
        return params.all().stream().map(ParameterView::of).toList();
    }

    @PutMapping("/system-parameters/{key}")
    public ParameterView updateParameter(@PathVariable String key, @Valid @RequestBody ParameterRequest req) {
        return ParameterView.of(params.update(key, req.value()));
    }
}
