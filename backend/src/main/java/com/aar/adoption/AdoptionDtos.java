package com.aar.adoption;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class AdoptionDtos {

    private AdoptionDtos() {
    }

    /** A01. sterilizationPledge bắt buộc nếu thú chưa triệt sản (checklist mục 5). */
    /**
     * phone, address: bắt buộc nếu tài khoản chưa có (ví dụ đăng ký bằng Google), được lưu vào tài khoản
     * để nhân viên liên hệ khi xét duyệt và hậu kiểm.
     */
    public record SubmitRequest(
        @NotNull Long petId,
        @NotNull HousingType housingType,
        @NotNull Boolean hasPetExperience,
        @Size(max = 2000) String motivation,
        Boolean sterilizationPledge,
        @Pattern(regexp = "^[0-9+]{9,15}$", message = "Số điện thoại không hợp lệ") String phone,
        @Size(max = 255) String address) {
    }

    public record CancelRequest(@Size(max = 1000) String reason) {
    }

    /** Duyệt đơn: ghi chú và ngày hẹn bàn giao (mục 2.1.4 bước 3). */
    public record ReviewRequest(@Size(max = 2000) String note, @FutureOrPresent LocalDate handoverDate) {
    }

    public record HandoverDateRequest(@NotNull @FutureOrPresent LocalDate handoverDate) {
    }

    public record RejectRequest(@NotBlank @Size(max = 2000) String note) {
    }

    public record PetBrief(Long id, String code, String name) {
    }

    public record ApplicantBrief(Long id, String fullName, String email, String phone, String address) {
    }

    public record ApplicationView(Long id, PetBrief pet, ApplicantBrief applicant, HousingType housingType,
                                  boolean hasPetExperience, String motivation, boolean sterilizationPledge,
                                  ApplicationStatus status, String reviewNote, OffsetDateTime reviewedAt,
                                  LocalDate handoverDate, String cancelReason, OffsetDateTime submittedAt) {
    }

    /**
     * Lịch sử nhận nuôi của người nộp đơn để cảnh báo nhân viên khi xét duyệt.
     * RECALLED: từng bị thu hồi thú do hậu kiểm Không đạt. RETURNED: từng tự trả lại thú.
     */
    public record ApplicantFlag(String kind, Long petId, String petCode, String petName, OffsetDateTime handedOverAt,
                                String note) {
    }

    /** S07: đơn kèm các đơn khác cho cùng thú để nhân viên so sánh. */
    public record ApplicationDetail(ApplicationView application, String petStatus,
                                    List<ApplicationView> otherApplications, List<ApplicantFlag> applicantHistory) {
    }
}
