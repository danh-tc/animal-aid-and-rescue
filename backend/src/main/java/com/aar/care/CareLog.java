package com.aar.care;

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
@Table(name = "care_logs")
public class CareLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private CareActivity activityType;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    private String note;

    @Column(name = "logged_at", nullable = false)
    private OffsetDateTime loggedAt;

    protected CareLog() {
    }

    public CareLog(Long petId, Long staffId, CareActivity activityType, BigDecimal weightKg, String note) {
        this.petId = petId;
        this.staffId = staffId;
        this.activityType = activityType;
        this.weightKg = weightKg;
        this.note = note;
        this.loggedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getStaffId() { return staffId; }
    public CareActivity getActivityType() { return activityType; }
    public BigDecimal getWeightKg() { return weightKg; }
    public String getNote() { return note; }
    public OffsetDateTime getLoggedAt() { return loggedAt; }
}
