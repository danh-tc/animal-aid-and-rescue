package com.aar.common.storage;

import com.aar.common.error.BusinessException;
import com.aar.handover.FollowUpRepository;
import com.aar.care.VetRequestRepository;
import com.aar.pet.PetRepository;
import com.aar.pet.PetStatus;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tải lên và tải về tệp đính kèm: ảnh thú (S05), phiếu khám (S06), ảnh hậu kiểm (S09).
 * Khách chỉ xem được ảnh của thú đang Sẵn sàng.
 */
@RestController
public class AttachmentController {

    private final StorageService storage;
    private final AttachmentRepository attachments;
    private final PetRepository pets;
    private final VetRequestRepository vetRequests;
    private final FollowUpRepository followUps;

    public AttachmentController(StorageService storage, AttachmentRepository attachments, PetRepository pets,
                                VetRequestRepository vetRequests, FollowUpRepository followUps) {
        this.storage = storage;
        this.attachments = attachments;
        this.pets = pets;
        this.vetRequests = vetRequests;
        this.followUps = followUps;
    }

    @PostMapping(value = "/api/staff/pets/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<AttachmentView> uploadPet(@PathVariable Long id, @RequestPart("files") List<MultipartFile> files) {
        if (!pets.existsById(id)) {
            throw BusinessException.notFound("Không tìm thấy thú");
        }
        return upload(AttachmentOwner.PET, id, files);
    }

    @PostMapping(value = "/api/staff/vet-requests/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<AttachmentView> uploadVet(@PathVariable Long id, @RequestPart("files") List<MultipartFile> files) {
        if (!vetRequests.existsById(id)) {
            throw BusinessException.notFound("Không tìm thấy yêu cầu thú y");
        }
        return upload(AttachmentOwner.VET_REQUEST, id, files);
    }

    @PostMapping(value = "/api/staff/follow-ups/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<AttachmentView> uploadFollowUp(@PathVariable Long id, @RequestPart("files") List<MultipartFile> files) {
        if (!followUps.existsById(id)) {
            throw BusinessException.notFound("Không tìm thấy lịch hậu kiểm");
        }
        return upload(AttachmentOwner.FOLLOW_UP, id, files);
    }

    /** Danh sách phiếu khám của một yêu cầu thú y (S06). */
    @GetMapping("/api/staff/vet-requests/{id}/attachments")
    public List<AttachmentView> vetAttachments(@PathVariable Long id) {
        return attachments.findByVetRequestIdOrderById(id).stream().map(AttachmentView::of).toList();
    }

    /** Ảnh của một lần hậu kiểm (S09). */
    @GetMapping("/api/staff/follow-ups/{id}/attachments")
    public List<AttachmentView> followUpAttachments(@PathVariable Long id) {
        return attachments.findByFollowUpIdOrderById(id).stream().map(AttachmentView::of).toList();
    }

    @GetMapping("/api/staff/attachments/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return serve(find(id));
    }

    @GetMapping("/api/public/pets/{petId}/photos/{id}")
    public ResponseEntity<Resource> publicPetPhoto(@PathVariable Long petId, @PathVariable Long id) {
        Attachment a = find(id);
        boolean visible = a.owner() == AttachmentOwner.PET && a.ownerId().equals(petId)
            && pets.findByIdAndStatus(petId, PetStatus.AVAILABLE).isPresent()
            && StorageService.mediaTypeOf(a).getType().equals("image");
        if (!visible) {
            throw BusinessException.notFound("Không tìm thấy ảnh");
        }
        return serve(a);
    }

    private List<AttachmentView> upload(AttachmentOwner owner, Long id, List<MultipartFile> files) {
        if (files == null || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw BusinessException.badRequest("NO_FILES", "Chưa chọn tệp");
        }
        return storage.store(owner, id, files).stream().map(AttachmentView::of).toList();
    }

    private Attachment find(Long id) {
        return attachments.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy tệp"));
    }

    private ResponseEntity<Resource> serve(Attachment a) {
        String name = a.getOriginalName() == null ? "tep-" + a.getId() : a.getOriginalName();
        return ResponseEntity.ok()
            .contentType(StorageService.mediaTypeOf(a))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + java.net.URLEncoder
                .encode(name, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"))
            .header("X-Content-Type-Options", "nosniff")
            .body(storage.load(a));
    }
}
