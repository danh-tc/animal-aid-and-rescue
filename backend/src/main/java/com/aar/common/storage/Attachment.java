package com.aar.common.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "pet_id")
    private Long petId;

    @Column(name = "vet_request_id")
    private Long vetRequestId;

    @Column(name = "follow_up_id")
    private Long followUpId;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    protected Attachment() {
    }

    public Attachment(AttachmentOwner owner, Long ownerId, String filePath, String originalName) {
        switch (owner) {
            case RESCUE_REPORT -> this.reportId = ownerId;
            case PET -> this.petId = ownerId;
            case VET_REQUEST -> this.vetRequestId = ownerId;
            case FOLLOW_UP -> this.followUpId = ownerId;
        }
        this.filePath = filePath;
        this.originalName = originalName;
        this.uploadedAt = OffsetDateTime.now();
    }

    public AttachmentOwner owner() {
        if (reportId != null) return AttachmentOwner.RESCUE_REPORT;
        if (petId != null) return AttachmentOwner.PET;
        if (vetRequestId != null) return AttachmentOwner.VET_REQUEST;
        return AttachmentOwner.FOLLOW_UP;
    }

    public Long ownerId() {
        if (reportId != null) return reportId;
        if (petId != null) return petId;
        if (vetRequestId != null) return vetRequestId;
        return followUpId;
    }

    public Long getId() { return id; }
    public String getFilePath() { return filePath; }
    public String getOriginalName() { return originalName; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
}
