package com.aar.common.state;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "status_histories")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(nullable = false)
    private String event;

    private String note;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    protected StatusHistory() {
    }

    public StatusHistory(EntityType entityType, Long entityId, String fromStatus, String toStatus,
                         String event, String note, Long changedBy) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.event = event;
        this.note = note;
        this.changedBy = changedBy;
        this.changedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public EntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getEvent() { return event; }
    public String getNote() { return note; }
    public Long getChangedBy() { return changedBy; }
    public OffsetDateTime getChangedAt() { return changedAt; }
}
