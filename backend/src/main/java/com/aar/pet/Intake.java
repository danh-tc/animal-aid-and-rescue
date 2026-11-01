package com.aar.pet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Lượt tiếp nhận: mỗi lần thú vào trung tâm (QĐ-03). Không xóa (RB-11). */
@Entity
@Table(name = "intakes")
public class Intake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntakeSource source;

    @Column(name = "rescue_case_id")
    private Long rescueCaseId;

    @Column(name = "returned_handover_id")
    private Long returnedHandoverId;

    @Column(name = "intake_at", nullable = false)
    private OffsetDateTime intakeAt;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(name = "initial_assessment")
    private String initialAssessment;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    protected Intake() {
    }

    private Intake(Long petId, IntakeSource source, Long rescueCaseId, Long returnedHandoverId,
                   String reason, Priority priority, String initialAssessment, Long createdBy) {
        this.petId = petId;
        this.source = source;
        this.rescueCaseId = rescueCaseId;
        this.returnedHandoverId = returnedHandoverId;
        this.reason = reason;
        this.priority = priority;
        this.initialAssessment = initialAssessment;
        this.createdBy = createdBy;
        this.intakeAt = OffsetDateTime.now();
    }

    public static Intake fromRescue(Long petId, Long caseId, String reason, Priority priority,
                                    String assessment, Long by) {
        return new Intake(petId, IntakeSource.RESCUE, caseId, null, reason, priority, assessment, by);
    }

    /** Tìm lại thú thất lạc (QĐ-16): lượt tiếp nhận mới trên hồ sơ cũ. */
    public static Intake fromFound(Long petId, String reason, Priority priority, String initialAssessment, Long by) {
        return new Intake(petId, IntakeSource.FOUND, null, null, reason, priority, initialAssessment, by);
    }

    public static Intake fromReturn(Long petId, Long handoverId, String reason, Priority priority,
                                    String assessment, Long by) {
        return new Intake(petId, IntakeSource.RETURN, null, handoverId, reason, priority, assessment, by);
    }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public IntakeSource getSource() { return source; }
    public Long getRescueCaseId() { return rescueCaseId; }
    public Long getReturnedHandoverId() { return returnedHandoverId; }
    public OffsetDateTime getIntakeAt() { return intakeAt; }
    public String getReason() { return reason; }
    public Priority getPriority() { return priority; }
    public String getInitialAssessment() { return initialAssessment; }
    public Long getCreatedBy() { return createdBy; }
}
