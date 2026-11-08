package com.aar.rescue;

import com.aar.common.storage.AttachmentView;
import com.aar.pet.Priority;
import com.aar.pet.Sex;
import com.aar.pet.Species;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class RescueDtos {

    private RescueDtos() {
    }

    public record ReportRequest(
        @NotBlank @Size(max = 150) String reporterName,
        @NotBlank @Pattern(regexp = "^[0-9+]{9,15}$", message = "Số điện thoại không hợp lệ") String reporterPhone,
        @NotBlank @Size(max = 255) String locationText,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotBlank @Size(max = 2000) String description) {
    }

    public record ReportReceipt(String code, ReportStatus status, OffsetDateTime createdAt) {
    }

    public record ReportSummary(Long id, String code, String reporterName, String reporterPhone, String locationText,
                                String description, ReportStatus status, OffsetDateTime createdAt) {

        static ReportSummary of(RescueReport r) {
            return new ReportSummary(r.getId(), r.getCode(), r.getReporterName(), r.getReporterPhone(),
                r.getLocationText(), r.getDescription(), r.getStatus(), r.getCreatedAt());
        }
    }

    public record ReportDetail(Long id, String code, String reporterName, String reporterPhone, String locationText,
                               BigDecimal latitude, BigDecimal longitude, String description, ReportStatus status,
                               Long handledBy, OffsetDateTime createdAt, OffsetDateTime handledAt,
                               List<AttachmentView> photos, List<CaseSummary> cases, List<OpenCase> openCases) {
    }

    public record OpenCase(CaseSummary rescueCase, Double distanceKm, List<String> reportCodes) {
    }

    public record MergeRequest(@NotNull Long caseId) {
    }

    public record ReportLookup(String code, ReportStatus status, OffsetDateTime createdAt, OffsetDateTime handledAt,
                               List<CaseProgress> cases) {
    }

    public record CaseProgress(CaseStatus status, OffsetDateTime closedAt, PetProgress pet) {
    }

    public record PetProgress(PetStage stage, String petName, Long petId) {
    }

    public enum PetStage { IN_CARE, LOOKING_FOR_HOME, ADOPTED, DECEASED, LOST }

    public record CreateCasesRequest(
        @NotNull @Min(1) @Max(20) Integer numberOfAnimals,
        Long assignedStaffId) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record AssignRequest(@NotNull Long staffId) {
    }

    public record NewPet(
        @Size(max = 100) String name,
        @NotNull Species species,
        Sex sex,
        @PastOrPresent LocalDate estimatedBirthDate,
        @Size(max = 100) String color,
        @Size(max = 2000) String description) {
    }

    public record NewIntake(
        @Size(max = 1000) String reason,
        @NotNull Priority priority,
        @Size(max = 2000) String initialAssessment) {
    }

    public record SucceedRequest(@Valid NewPet pet, Long lostPetId, @NotNull @Valid NewIntake intake) {
    }

    public record CaseSummary(Long id, String locationText, CaseStatus status, Long assignedStaffId,
                              String failureReason, OffsetDateTime createdAt, OffsetDateTime closedAt) {

        static CaseSummary of(RescueCase c) {
            return new CaseSummary(c.getId(), c.getLocationText(), c.getStatus(), c.getAssignedStaffId(),
                c.getFailureReason(), c.getCreatedAt(), c.getClosedAt());
        }
    }

    public record CaseDetail(Long id, String locationText, CaseStatus status, Long assignedStaffId,
                             String failureReason, Long createdBy, OffsetDateTime createdAt, OffsetDateTime closedAt,
                             List<ReportSummary> reports, Long petId, String petCode) {
    }

    public record SucceedResult(CaseSummary rescueCase, Long petId, String petCode, Long intakeId,
                                boolean urgentVetRequestSuggested) {
    }
}
