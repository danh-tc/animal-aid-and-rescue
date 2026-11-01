package com.aar.pet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Hồ sơ thú duy nhất, giữ trạng thái hiện tại (báo cáo mục 4.1).
 * Trạng thái chỉ đổi qua StateTransitionService, không có setter công khai cho status.
 */
@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sex sex = Sex.UNKNOWN;

    @Column(name = "estimated_birth_date")
    private LocalDate estimatedBirthDate;

    private String color;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetStatus status;

    @Enumerated(EnumType.STRING)
    private PetArea area;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "is_vaccinated", nullable = false)
    private boolean vaccinated;

    @Column(name = "is_dewormed", nullable = false)
    private boolean dewormed;

    @Column(name = "is_sterilized", nullable = false)
    private boolean sterilized;

    @Column(name = "temperament_ok", nullable = false)
    private boolean temperamentOk;

    @Column(name = "eligibility_checked_at")
    private OffsetDateTime eligibilityCheckedAt;

    @Column(name = "eligibility_checked_by")
    private Long eligibilityCheckedBy;

    @Version
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Pet() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    /** Chỉ gọi với trạng thái đích do StateTransitionService trả về. */
    public void changeStatus(PetStatus newStatus) {
        this.status = newStatus;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Species getSpecies() { return species; }
    public Sex getSex() { return sex; }
    public LocalDate getEstimatedBirthDate() { return estimatedBirthDate; }
    public String getColor() { return color; }
    public String getDescription() { return description; }
    public PetStatus getStatus() { return status; }
    public PetArea getArea() { return area; }
    public String getCurrentLocation() { return currentLocation; }
    public boolean isVaccinated() { return vaccinated; }
    public boolean isDewormed() { return dewormed; }
    public boolean isSterilized() { return sterilized; }
    public boolean isTemperamentOk() { return temperamentOk; }
    public OffsetDateTime getEligibilityCheckedAt() { return eligibilityCheckedAt; }
    public Long getEligibilityCheckedBy() { return eligibilityCheckedBy; }
    public Integer getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
