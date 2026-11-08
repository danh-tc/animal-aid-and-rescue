package com.aar.handover;

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

@Entity
@Table(name = "handovers")
public class Handover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true, updatable = false)
    private Long applicationId;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private Long petId;

    @Column(name = "staff_id", nullable = false, updatable = false)
    private Long staffId;

    @Column(name = "handed_over_at", nullable = false, updatable = false)
    private OffsetDateTime handedOverAt;

    @Column(name = "donation_amount", nullable = false)
    private BigDecimal donationAmount;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_status", nullable = false)
    private HandoverStatus status;

    protected Handover() {
    }

    public Handover(Long applicationId, Long petId, Long staffId, BigDecimal donationAmount, String note,
                    HandoverStatus status) {
        this.applicationId = applicationId;
        this.petId = petId;
        this.staffId = staffId;
        this.donationAmount = donationAmount;
        this.note = note;
        this.status = status;
        this.handedOverAt = OffsetDateTime.now();
    }

    public void changeStatus(HandoverStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public Long getPetId() { return petId; }
    public Long getStaffId() { return staffId; }
    public OffsetDateTime getHandedOverAt() { return handedOverAt; }
    public BigDecimal getDonationAmount() { return donationAmount; }
    public String getNote() { return note; }
    public HandoverStatus getStatus() { return status; }
}
