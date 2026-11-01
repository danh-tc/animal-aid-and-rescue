package com.aar.system;

import com.aar.common.error.BusinessException;
import com.aar.common.security.AuthUser;
import com.aar.common.security.CurrentUser;
import com.aar.system.AuthDtos.MeView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thông tin tài khoản đang đăng nhập, dùng cho frontend sau khi tải lại trang. */
@RestController
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/me")
    public MeView me(@CurrentUser AuthUser current) {
        return users.findById(current.id()).map(MeView::of)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tài khoản"));
    }
}
