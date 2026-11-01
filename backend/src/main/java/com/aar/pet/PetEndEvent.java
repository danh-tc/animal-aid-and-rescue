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

/** Bảng pet_end_events: qua đời, thất lạc, chuyển cơ sở. Chuyển cơ sở bắt buộc có nơi nhận (CHECK ck_end_transfer_dest). */
@Entity
@Table(name = "pet_end_events")
public class PetEndEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EndEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    private String detail;

    @Column(name = "destination_org")
    private String destinationOrg;

    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    protected PetEndEvent() {
    }

    public PetEndEvent(Long petId, EndEventType eventType, OffsetDateTime occurredAt, String detail,
                       String destinationOrg, Long recordedBy) {
        this.petId = petId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.detail = detail;
        this.destinationOrg = destinationOrg;
        this.recordedBy = recordedBy;
    }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public EndEventType getEventType() { return eventType; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public String getDetail() { return detail; }
    public String getDestinationOrg() { return destinationOrg; }
    public Long getRecordedBy() { return recordedBy; }
}
