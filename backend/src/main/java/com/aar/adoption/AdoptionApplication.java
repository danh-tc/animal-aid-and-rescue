package com.aar.adoption;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "adoption_applications")
public class AdoptionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private Long petId;

    @Column(name = "applicant_id", nullable = false, updatable = false)
    private Long applicantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_type", nullable = false)
    private HousingType housingType;

    @Column(name = "has_pet_experience", nullable = false)
    private boolean hasPetExperience;

    private String motivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "handover_date")
    private java.time.LocalDate handoverDate;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Version
    private Integer version;

    protected AdoptionApplication() {
    }

    public AdoptionApplication(Long petId, Long applicantId, HousingType housingType, boolean hasPetExperience,
                               String motivation, ApplicationStatus status) {
        this.petId = petId;
        this.applicantId = applicantId;
        this.housingType = housingType;
        this.hasPetExperience = hasPetExperience;
        this.motivation = motivation;
        this.status = status;
        this.submittedAt = OffsetDateTime.now();
    }

    public void changeStatus(ApplicationStatus status) {
        this.status = status;
    }

    public void recordReview(String note, Long by) {
        this.reviewNote = note;
        this.reviewedBy = by;
        this.reviewedAt = OffsetDateTime.now();
    }

    public void recordCancel(String reason) {
        this.cancelReason = reason;
    }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getApplicantId() { return applicantId; }
    public HousingType getHousingType() { return housingType; }
    public boolean isHasPetExperience() { return hasPetExperience; }
    public String getMotivation() { return motivation; }
    public ApplicationStatus getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public Long getReviewedBy() { return reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public java.time.LocalDate getHandoverDate() { return handoverDate; }

    public void scheduleHandover(java.time.LocalDate date) {
        this.handoverDate = date;
    }
    public String getCancelReason() { return cancelReason; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
}
