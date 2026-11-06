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

    /** P03: Khách báo cứu hộ, không cần đăng nhập. */
    public record ReportRequest(
        @NotBlank @Size(max = 150) String reporterName,
        @NotBlank @Pattern(regexp = "^[0-9+]{9,15}$", message = "Số điện thoại không hợp lệ") String reporterPhone,
        @NotBlank @Size(max = 255) String locationText,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotBlank @Size(max = 2000) String description) {
    }

    /** Khách chỉ nhận lại mã để tra cứu, không lộ dữ liệu nội bộ. */
    public record ReportReceipt(String code, ReportStatus status, OffsetDateTime createdAt) {
    }

    public record ReportSummary(Long id, String code, String reporterName, String reporterPhone, String locationText,
                                String description, ReportStatus status, OffsetDateTime createdAt) {

        static ReportSummary of(RescueReport r) {
            return new ReportSummary(r.getId(), r.getCode(), r.getReporterName(), r.getReporterPhone(),
                r.getLocationText(), r.getDescription(), r.getStatus(), r.getCreatedAt());
        }
    }

    /**
     * openCases: các ca đang mở để nhân viên kiểm tra trùng (bước 2 của UC-07), ca gần nhất trước.
     * distanceKm chỉ có khi cả báo cáo này và báo cáo gốc của ca đều có tọa độ.
     */
    public record ReportDetail(Long id, String code, String reporterName, String reporterPhone, String locationText,
                               BigDecimal latitude, BigDecimal longitude, String description, ReportStatus status,
                               Long handledBy, OffsetDateTime createdAt, OffsetDateTime handledAt,
                               List<AttachmentView> photos, List<CaseSummary> cases, List<OpenCase> openCases) {
    }

    public record OpenCase(CaseSummary rescueCase, Double distanceKm, List<String> reportCodes) {
    }

    /** Ngoại lệ 2a của UC-07: gộp báo cáo trùng vào một ca đang mở (QĐ-01). */
    public record MergeRequest(@NotNull Long caseId) {
    }

    /** P04: Khách tra cứu bằng mã và số điện thoại (RB-15). Chỉ trả tiến độ, không lộ dữ liệu nội bộ. */
    public record ReportLookup(String code, ReportStatus status, OffsetDateTime createdAt, OffsetDateTime handledAt,
                               List<CaseProgress> cases) {
    }

    /** pet có khi ca Thành công: người báo theo dõi tiếp con vật mình đã báo. */
    public record CaseProgress(CaseStatus status, OffsetDateTime closedAt, PetProgress pet) {
    }

    /**
     * Giai đoạn công khai của thú, gộp từ trạng thái nội bộ để không lộ chi tiết điều trị hay người nhận nuôi.
     * petId chỉ có khi thú đang tìm nhà, để mở trang hồ sơ công khai.
     */
    public record PetProgress(PetStage stage, String petName, Long petId) {
    }

    public enum PetStage { IN_CARE, LOOKING_FOR_HOME, ADOPTED, DECEASED, LOST }

    /** Tách báo cáo nhiều thú thành nhiều ca (QĐ-01). assignedStaffId có thì phân công luôn. */
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

    /** S-1 của UC-07: tạo hồ sơ thú và lượt tiếp nhận RESCUE trong cùng giao dịch. */
    /**
     * S-1 của UC-07. Cứu thú mới thì gửi pet. Nhánh S-1b: nhân viên đối chiếu và xác nhận đây là thú thất lạc
     * của trung tâm thì gửi lostPetId thay cho pet, hệ thống tạo lượt tìm lại trên hồ sơ cũ (QĐ-16).
     */
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

    /** urgentVetRequestSuggested: đánh giá Khẩn cấp thì nhắc tạo yêu cầu thú y ngay (S-1a của UC-07). */
    public record SucceedResult(CaseSummary rescueCase, Long petId, String petCode, Long intakeId,
                                boolean urgentVetRequestSuggested) {
    }
}
