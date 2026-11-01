package com.aar.care;

import com.aar.pet.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Yêu cầu thú y gửi cơ sở thú y (thực thể ngoài). Nhân viên nhập hộ kết quả. */
@Entity
@Table(name = "vet_requests")
public class VetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private Long petId;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_mode", nullable = false)
    private CareMode careMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VetRequestStatus status;

    @Column(name = "diagnosis_summary")
    private String diagnosisSummary;

    @Column(name = "treatment_summary")
    private String treatmentSummary;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    @Column(name = "aftercare_instructions")
    private String aftercareInstructions;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected VetRequest() {
    }

    public VetRequest(Long petId, Long clinicId, String reason, Priority priority, CareMode careMode,
                      VetRequestStatus status, Long createdBy) {
        this.petId = petId;
        this.clinicId = clinicId;
        this.reason = reason;
        this.priority = priority;
        this.careMode = careMode;
        this.status = status;
        this.createdBy = createdBy;
        this.requestedAt = OffsetDateTime.now();
    }

    public void changeStatus(VetRequestStatus status) {
        this.status = status;
        if (status == VetRequestStatus.COMPLETED) {
            this.completedAt = OffsetDateTime.now();
        }
    }

    public void recordResult(String diagnosis, String treatment, BigDecimal totalCost, String aftercare) {
        this.diagnosisSummary = diagnosis;
        this.treatmentSummary = treatment;
        this.totalCost = totalCost;
        this.aftercareInstructions = aftercare;
    }

    public void recordCancel(String reason) {
        this.cancelReason = reason;
    }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getClinicId() { return clinicId; }
    public String getReason() { return reason; }
    public Priority getPriority() { return priority; }
    public CareMode getCareMode() { return careMode; }
    public VetRequestStatus getStatus() { return status; }
    public String getDiagnosisSummary() { return diagnosisSummary; }
    public String getTreatmentSummary() { return treatmentSummary; }
    public BigDecimal getTotalCost() { return totalCost; }
    public String getAftercareInstructions() { return aftercareInstructions; }
    public String getCancelReason() { return cancelReason; }
    public Long getCreatedBy() { return createdBy; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
}
