package com.aar.common.storage;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByReportIdOrderById(Long reportId);

    List<Attachment> findByPetIdOrderById(Long petId);

    List<Attachment> findByVetRequestIdOrderById(Long vetRequestId);

    List<Attachment> findByFollowUpIdOrderById(Long followUpId);
}
