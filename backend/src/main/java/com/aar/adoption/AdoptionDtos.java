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

    public record SubmitRequest(
        @NotNull Long petId,
        @NotNull HousingType housingType,
        @NotNull Boolean hasPetExperience,
        @Size(max = 2000) String motivation,
        @Pattern(regexp = "^[0-9+]{9,15}$", message = "Số điện thoại không hợp lệ") String phone,
        @Size(max = 255) String address) {
    }

    public record CancelRequest(@Size(max = 1000) String reason) {
    }

    public record ReviewRequest(@Size(max = 2000) String note, @FutureOrPresent LocalDate handoverDate) {
    }

    public record HandoverDateRequest(@NotNull @FutureOrPresent LocalDate handoverDate) {
    }

    public record RejectRequest(@NotBlank @Size(max = 2000) String note) {
    }

    public record StaffCancelRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record PetBrief(Long id, String code, String name) {
    }

    public record ApplicantBrief(Long id, String fullName, String email, String phone, String address) {
    }

    public record ApplicationView(Long id, PetBrief pet, ApplicantBrief applicant, HousingType housingType,
                                  boolean hasPetExperience, String motivation,
                                  ApplicationStatus status, String reviewNote, OffsetDateTime reviewedAt,
                                  LocalDate handoverDate, String cancelReason, OffsetDateTime submittedAt) {
    }

    public record ApplicantFlag(String kind, Long petId, String petCode, String petName, OffsetDateTime handedOverAt,
                                String note) {
    }

    public record ApplicationDetail(ApplicationView application, String petStatus,
                                    List<ApplicationView> otherApplications, List<ApplicantFlag> applicantHistory) {
    }
}
