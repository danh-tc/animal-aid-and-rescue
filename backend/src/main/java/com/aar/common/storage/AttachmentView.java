package com.aar.common.storage;

import java.time.OffsetDateTime;

public record AttachmentView(Long id, String originalName, OffsetDateTime uploadedAt) {

    public static AttachmentView of(Attachment a) {
        return new AttachmentView(a.getId(), a.getOriginalName(), a.getUploadedAt());
    }
}
