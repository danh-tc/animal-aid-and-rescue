package com.aar.system;

/** Xác thực ID token do Google Identity Services cấp cho trình duyệt. Tách interface để test thay bằng bản giả. */
public interface GoogleTokenVerifier {

    GoogleIdentity verify(String idToken);

    /** Thông tin đã được Google ký. emailVerified = false thì không dùng để đăng nhập. */
    record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
    }
}
