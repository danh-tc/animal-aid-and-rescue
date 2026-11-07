package com.aar.handover;

import com.aar.pet.EndEventType;
import com.aar.pet.PetEndEvent;
import com.aar.pet.PetStatus;
import com.aar.pet.Priority;
import jakarta.validation.Valid;
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

public final class HandoverDtos {

    private HandoverDtos() {
    }

    /** S08. Khoản ủng hộ thuộc đợt 2 nhưng cột đã có nên nhận luôn, mặc định 0. */
    public record HandoverRequest(
        @Size(max = 2000) String note,
        @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal donationAmount) {
    }

    public record FollowUpView(Long id, Long handoverId, int sequenceNo, boolean additional, LocalDate dueDate,
                               FollowUpStatus status, FollowUpResult result, ContactMethod contactMethod,
                               Boolean healthOk, Boolean housingOk,
                               Boolean commitmentOk, Boolean petPresent, Boolean ownerReachable, String reason,
                               Long performedBy, OffsetDateTime performedAt) {

        static FollowUpView of(FollowUp f) {
            return new FollowUpView(f.getId(), f.getHandoverId(), f.getSequenceNo(), f.isAdditional(), f.getDueDate(),
                f.getStatus(), f.getResult(), f.getContactMethod(), f.getHealthOk(), f.getHousingOk(), f.getCommitmentOk(),
                f.getPetPresent(), f.getOwnerReachable(), f.getReason(), f.getPerformedBy(), f.getPerformedAt());
        }
    }

    public record HandoverView(Long id, Long applicationId, Long petId, String petCode, String petName,
                               Long ownerId, String ownerName, String ownerPhone, OffsetDateTime handedOverAt,
                               BigDecimal donationAmount, String note, HandoverStatus status,
                               List<FollowUpView> followUps) {
    }

    /** S09: lịch hậu kiểm kèm thông tin liên hệ chủ nuôi. */
    public record ScheduledFollowUp(FollowUpView followUp, Long petId, String petCode, String petName,
                                    String ownerName, String ownerPhone, String ownerAddress) {
    }

    /** 5 tiêu chí (QĐ-12) và hình thức liên hệ (mục 2.1.5). Cần theo dõi và Không đạt bắt buộc ghi lý do (RB-09). */
    public record FollowUpResultRequest(
        @NotNull FollowUpResult result,
        @NotNull ContactMethod contactMethod,
        @NotNull Boolean healthOk,
        @NotNull Boolean housingOk,
        @NotNull Boolean commitmentOk,
        @NotNull Boolean petPresent,
        @NotNull Boolean ownerReachable,
        @Size(max = 2000) String reason) {
    }

    public record FollowUpOutcome(FollowUpView followUp, HandoverStatus handoverStatus, FollowUpView additionalFollowUp) {
    }

    /** QĐ-18: Quản lý đóng thu hồi, bắt buộc ghi lý do (RB-21). */
    public record CloseRecallRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    /** S10: tiếp nhận trả lại hoặc thu hồi. */
    public record ReturnRequest(
        @NotBlank @Size(max = 1000) String reason,
        @NotNull Priority priority,
        @Size(max = 2000) String initialAssessment) {
    }

    public record ReturnResult(Long petId, String petStatus, Long intakeId, Long handoverId, int cancelledFollowUps) {
    }

    /**
     * S10: ghi sự kiện kết thúc (mục 2.1.6). Thất lạc cần nơi và hoàn cảnh mất.
     * incurredCosts: chi phí đã phát sinh của từng yêu cầu thú y đang mở (QĐ-14), yêu cầu không nhập thì ghi 0.
     */
    public record EndEventRequest(
        @NotNull EndEventType eventType,
        @PastOrPresent OffsetDateTime occurredAt,
        @Size(max = 2000) String detail,
        @Valid List<IncurredCost> incurredCosts) {
    }

    public record IncurredCost(
        @NotNull Long vetRequestId,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal amount) {
    }

    public record EndEventView(Long id, EndEventType eventType, OffsetDateTime occurredAt, String detail,
                               Long recordedBy) {
        static EndEventView of(PetEndEvent e) {
            return new EndEventView(e.getId(), e.getEventType(), e.getOccurredAt(), e.getDetail(), e.getRecordedBy());
        }
    }

    /** Kết quả kèm số việc đang mở đã bị đóng theo QĐ-15. */
    public record EndEventResult(EndEventView event, PetStatus petStatus, int cancelledVetRequests,
                                 int cancelledApplications, int cancelledFollowUps, boolean handoverClosed) {
    }

    /** QĐ-16: thú thất lạc tìm lại được, tạo lượt tiếp nhận FOUND. */
    public record FoundRequest(
        @NotBlank @Size(max = 1000) String reason,
        @NotNull Priority priority,
        @Size(max = 2000) String initialAssessment) {
    }

    public record FoundResult(Long petId, PetStatus petStatus, Long intakeId) {
    }

    /** QĐ-14: 3 con số chi phí của thú, hiển thị khi bàn giao (bước 2 của UC-11). */
    public record PetCosts(Long petId, BigDecimal totalTreatmentCost, BigDecimal totalDonated, BigDecimal outstanding) {
    }
}
