package com.aar.handover;

import com.aar.pet.Priority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
                               FollowUpStatus status, FollowUpResult result, Boolean healthOk, Boolean housingOk,
                               Boolean commitmentOk, Boolean petPresent, Boolean ownerReachable, String reason,
                               Long performedBy, OffsetDateTime performedAt) {

        static FollowUpView of(FollowUp f) {
            return new FollowUpView(f.getId(), f.getHandoverId(), f.getSequenceNo(), f.isAdditional(), f.getDueDate(),
                f.getStatus(), f.getResult(), f.getHealthOk(), f.getHousingOk(), f.getCommitmentOk(),
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

    /** 5 tiêu chí (QĐ-12). Cần theo dõi và Không đạt bắt buộc ghi lý do (RB-09). */
    public record FollowUpResultRequest(
        @NotNull FollowUpResult result,
        @NotNull Boolean healthOk,
        @NotNull Boolean housingOk,
        @NotNull Boolean commitmentOk,
        @NotNull Boolean petPresent,
        @NotNull Boolean ownerReachable,
        @Size(max = 2000) String reason) {
    }

    public record FollowUpOutcome(FollowUpView followUp, HandoverStatus handoverStatus, FollowUpView additionalFollowUp) {
    }

    /** S10: tiếp nhận trả lại hoặc thu hồi. */
    public record ReturnRequest(
        @NotBlank @Size(max = 1000) String reason,
        @NotNull Priority priority,
        @Size(max = 2000) String initialAssessment) {
    }

    public record ReturnResult(Long petId, String petStatus, Long intakeId, Long handoverId, int cancelledFollowUps) {
    }
}
