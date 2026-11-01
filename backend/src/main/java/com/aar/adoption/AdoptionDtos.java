package com.aar.adoption;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public final class AdoptionDtos {

    private AdoptionDtos() {
    }

    /** A01. sterilizationPledge bắt buộc nếu thú chưa triệt sản (checklist mục 5). */
    public record SubmitRequest(
        @NotNull Long petId,
        @NotNull HousingType housingType,
        @NotNull Boolean hasPetExperience,
        @Size(max = 2000) String motivation,
        Boolean sterilizationPledge) {
    }

    public record CancelRequest(@Size(max = 1000) String reason) {
    }

    public record ReviewRequest(@Size(max = 2000) String note) {
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
                                  String cancelReason, OffsetDateTime submittedAt) {
    }

    /** S07: đơn kèm các đơn khác cho cùng thú để nhân viên so sánh. */
    public record ApplicationDetail(ApplicationView application, String petStatus,
                                    List<ApplicationView> otherApplications) {
    }
}
