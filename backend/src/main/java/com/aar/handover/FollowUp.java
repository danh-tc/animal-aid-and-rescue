package com.aar.handover;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Một lần hậu kiểm theo 5 tiêu chí (QĐ-12). */
@Entity
@Table(name = "follow_ups")
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handover_id", nullable = false, updatable = false)
    private Long handoverId;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private Short sequenceNo;

    @Column(name = "is_additional", nullable = false, updatable = false)
    private boolean additional;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpStatus status;

    @Enumerated(EnumType.STRING)
    private FollowUpResult result;

    @Column(name = "health_ok")
    private Boolean healthOk;

    @Column(name = "housing_ok")
    private Boolean housingOk;

    @Column(name = "commitment_ok")
    private Boolean commitmentOk;

    @Column(name = "pet_present")
    private Boolean petPresent;

    @Column(name = "owner_reachable")
    private Boolean ownerReachable;

    private String reason;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "performed_at")
    private OffsetDateTime performedAt;

    protected FollowUp() {
    }

    public FollowUp(Long handoverId, int sequenceNo, boolean additional, LocalDate dueDate) {
        this.handoverId = handoverId;
        this.sequenceNo = (short) sequenceNo;
        this.additional = additional;
        this.dueDate = dueDate;
        this.status = FollowUpStatus.SCHEDULED;
    }

    public void recordResult(FollowUpResult result, boolean healthOk, boolean housingOk, boolean commitmentOk,
                             boolean petPresent, boolean ownerReachable, String reason, Long by) {
        this.status = FollowUpStatus.DONE;
        this.result = result;
        this.healthOk = healthOk;
        this.housingOk = housingOk;
        this.commitmentOk = commitmentOk;
        this.petPresent = petPresent;
        this.ownerReachable = ownerReachable;
        this.reason = reason;
        this.performedBy = by;
        this.performedAt = OffsetDateTime.now();
    }

    public void cancel() {
        this.status = FollowUpStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public Long getHandoverId() { return handoverId; }
    public int getSequenceNo() { return sequenceNo; }
    public boolean isAdditional() { return additional; }
    public LocalDate getDueDate() { return dueDate; }
    public FollowUpStatus getStatus() { return status; }
    public FollowUpResult getResult() { return result; }
    public Boolean getHealthOk() { return healthOk; }
    public Boolean getHousingOk() { return housingOk; }
    public Boolean getCommitmentOk() { return commitmentOk; }
    public Boolean getPetPresent() { return petPresent; }
    public Boolean getOwnerReachable() { return ownerReachable; }
    public String getReason() { return reason; }
    public Long getPerformedBy() { return performedBy; }
    public OffsetDateTime getPerformedAt() { return performedAt; }
}
