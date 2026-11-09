package com.aar.system;

import com.aar.common.Pages;
import com.aar.common.PageResponse;
import com.aar.common.error.BusinessException;
import com.aar.system.AdminDtos.ClinicRequest;
import com.aar.system.AdminDtos.ClinicView;
import com.aar.system.AdminDtos.CreateUserRequest;
import com.aar.system.AdminDtos.UpdateUserRequest;
import com.aar.system.AdminDtos.UserAdminView;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {

    private final UserRepository users;
    private final VetClinicRepository clinics;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository users, VetClinicRepository clinics, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.clinics = clinics;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAdminView> listUsers(Role role, int page, int size) {
        var pageable = Pages.of(page, size, Sort.by("id"));
        Page<User> result = role == null ? users.findAll(pageable) : users.findByRole(role, pageable);
        return PageResponse.of(result, UserAdminView::of);
    }

    public UserAdminView createUser(CreateUserRequest req) {
        if (req.role() == Role.ADOPTER) {
            throw BusinessException.badRequest("INVALID_ROLE", "Người nhận nuôi tự đăng ký tài khoản, Quản lý chỉ tạo được tài khoản Nhân viên hoặc Quản lý");
        }
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw BusinessException.conflict("EMAIL_TAKEN", "Email đã được sử dụng");
        }
        User user = users.save(new User(req.email().toLowerCase(), passwordEncoder.encode(req.password()),
            req.fullName(), req.phone(), req.role()));
        return UserAdminView.of(user);
    }

    public UserAdminView updateUser(Long id, UpdateUserRequest req, Long currentUserId) {
        User user = users.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy tài khoản"));
        if (id.equals(currentUserId)
            && (Boolean.FALSE.equals(req.active()) || (req.role() != null && req.role() != user.getRole()))) {
            throw BusinessException.badRequest("SELF_LOCKOUT", "Không thể tự khóa hoặc tự đổi vai trò của mình");
        }
        if (req.role() != null && (req.role() == Role.ADOPTER) != (user.getRole() == Role.ADOPTER)) {
            throw BusinessException.badRequest("INVALID_ROLE", "Không đổi được tài khoản Người nhận nuôi thành tài khoản nhân viên và ngược lại");
        }
        user.updateProfile(req.fullName() != null ? req.fullName() : user.getFullName(),
            req.phone() != null ? req.phone() : user.getPhone(),
            req.address() != null ? req.address() : user.getAddress());
        if (req.role() != null) {
            user.changeRole(req.role());
        }
        if (req.active() != null) {
            user.changeActive(req.active());
        }
        if (req.password() != null) {
            user.changePasswordHash(passwordEncoder.encode(req.password()));
        }
        return UserAdminView.of(user);
    }

    @Transactional(readOnly = true)
    public List<ClinicView> listClinics() {
        return clinics.findAllByOrderByName().stream().map(ClinicView::of).toList();
    }

    public ClinicView createClinic(ClinicRequest req) {
        VetClinic clinic = new VetClinic(req.name(), req.address(), req.phone());
        if (Boolean.FALSE.equals(req.active())) {
            clinic.update(req.name(), req.address(), req.phone(), false);
        }
        return ClinicView.of(clinics.save(clinic));
    }

    public ClinicView updateClinic(Long id, ClinicRequest req) {
        VetClinic clinic = clinics.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy cơ sở thú y"));
        clinic.update(req.name(), req.address(), req.phone(), req.active() == null ? clinic.isActive() : req.active());
        return ClinicView.of(clinic);
    }
}
