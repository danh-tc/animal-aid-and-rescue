package com.aar.care;

import com.aar.care.EligibilityService.Checklist;
import com.aar.common.storage.AttachmentView;
import com.aar.pet.Intake;
import com.aar.pet.IntakeSource;
import com.aar.pet.Pet;
import com.aar.pet.PetArea;
import com.aar.pet.PetStatus;
import com.aar.pet.Priority;
import com.aar.pet.Sex;
import com.aar.pet.Species;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class CareDtos {

    private CareDtos() {
    }

    public record PetSummary(Long id, String code, String name, Species species, Sex sex, PetStatus status,
                             PetArea area, String currentLocation, OffsetDateTime createdAt) {

        public static PetSummary of(Pet p) {
            return new PetSummary(p.getId(), p.getCode(), p.getName(), p.getSpecies(), p.getSex(), p.getStatus(),
                p.getArea(), p.getCurrentLocation(), p.getCreatedAt());
        }
    }

    public record IntakeView(Long id, IntakeSource source, Long rescueCaseId, Long returnedHandoverId,
                             OffsetDateTime intakeAt, String reason, Priority priority, String initialAssessment) {

        public static IntakeView of(Intake i) {
            return new IntakeView(i.getId(), i.getSource(), i.getRescueCaseId(), i.getReturnedHandoverId(),
                i.getIntakeAt(), i.getReason(), i.getPriority(), i.getInitialAssessment());
        }
    }

    /** S05: hồ sơ thú kèm lượt tiếp nhận, checklist tính ra, yêu cầu thú y và ảnh. */
    public record PetDetail(Long id, String code, String name, Species species, Sex sex,
                            LocalDate estimatedBirthDate, String color, String description, PetStatus status,
                            PetArea area, String currentLocation, boolean vaccinated, boolean dewormed,
                            boolean sterilized, boolean temperamentOk, OffsetDateTime eligibilityCheckedAt,
                            Long eligibilityCheckedBy, Integer version, OffsetDateTime createdAt,
                            List<IntakeView> intakes, Checklist checklist, List<VetRequestView> vetRequests,
                            List<AttachmentView> photos) {
    }

    /** Trường null nghĩa là giữ nguyên. Không có trường trạng thái (RB-12). */
    public record UpdatePetRequest(
        @Size(max = 100) String name,
        Sex sex,
        @PastOrPresent LocalDate estimatedBirthDate,
        @Size(max = 100) String color,
        @Size(max = 2000) String description,
        PetArea area,
        @Size(max = 255) String currentLocation) {
    }

    public record CareLogRequest(
        @NotNull CareActivity activityType,
        @DecimalMin(value = "0.01") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
        @Size(max = 2000) String note) {
    }

    public record CareLogView(Long id, Long staffId, CareActivity activityType, BigDecimal weightKg, String note,
                              OffsetDateTime loggedAt) {

        public static CareLogView of(CareLog l) {
            return new CareLogView(l.getId(), l.getStaffId(), l.getActivityType(), l.getWeightKg(), l.getNote(),
                l.getLoggedAt());
        }
    }

    public record EligibilityRequest(
        @NotNull Boolean vaccinated,
        @NotNull Boolean dewormed,
        @NotNull Boolean sterilized,
        @NotNull Boolean temperamentOk) {
    }

    /** Đủ 6 mục thì thú đã sang AVAILABLE, thiếu thì missing liệt kê các mục còn thiếu. */
    public record EligibilityResult(PetStatus status, Checklist checklist, List<String> missing) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record IneligibleResult(PetStatus status, int cancelledApplications) {
    }

    public record VetRequestCreate(
        @NotNull Long clinicId,
        @NotBlank @Size(max = 1000) String reason,
        @NotNull Priority priority,
        @NotNull CareMode careMode) {
    }

    public record VetRequestComplete(
        @Size(max = 2000) String diagnosisSummary,
        @Size(max = 2000) String treatmentSummary,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal totalCost,
        @Size(max = 2000) String aftercareInstructions) {
    }

    public record VetRequestView(Long id, Long petId, Long clinicId, String clinicName, String reason,
                                 Priority priority, CareMode careMode, VetRequestStatus status,
                                 String diagnosisSummary, String treatmentSummary, BigDecimal totalCost,
                                 String aftercareInstructions, String cancelReason, OffsetDateTime requestedAt,
                                 OffsetDateTime completedAt) {
    }

    /** Kết quả thao tác trên yêu cầu thú y kèm trạng thái mới của thú (QĐ-06). */
    public record VetRequestResult(VetRequestView request, PetStatus petStatus, int cancelledApplications) {
    }

    public record HistoryView(String fromStatus, String toStatus, String event, String note, Long changedBy,
                              OffsetDateTime changedAt) {
    }
}
