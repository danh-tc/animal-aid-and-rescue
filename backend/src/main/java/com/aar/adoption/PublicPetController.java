package com.aar.adoption;

import com.aar.common.PageResponse;
import com.aar.common.error.BusinessException;
import com.aar.pet.Pet;
import com.aar.pet.PetRepository;
import com.aar.pet.PetStatus;
import com.aar.pet.Sex;
import com.aar.pet.Species;
import com.aar.common.storage.Attachment;
import com.aar.common.storage.AttachmentRepository;
import com.aar.common.storage.StorageService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Màn hình P01, P02: chỉ công khai thú đang Sẵn sàng nhận nuôi. */
@RestController
@RequestMapping("/api/public/pets")
@Transactional(readOnly = true)
public class PublicPetController {

    private static final int MAX_PAGE_SIZE = 50;

    private final PetRepository pets;
    private final AttachmentRepository attachments;

    public PublicPetController(PetRepository pets, AttachmentRepository attachments) {
        this.pets = pets;
        this.attachments = attachments;
    }

    private PublicPet toPublic(Pet p) {
        List<Long> photoIds = attachments.findByPetIdOrderById(p.getId()).stream()
            .filter(a -> StorageService.mediaTypeOf(a).getType().equals("image"))
            .map(Attachment::getId).toList();
        return PublicPet.of(p, photoIds);
    }

    /** photoIds: tải ảnh qua /api/public/pets/{id}/photos/{photoId}. sterilized để A01 biết có cần cam kết triệt sản. */
    public record PublicPet(Long id, String code, String name, Species species, Sex sex,
                            LocalDate estimatedBirthDate, String color, String description, boolean sterilized,
                            List<Long> photoIds) {

        static PublicPet of(Pet p, List<Long> photoIds) {
            return new PublicPet(p.getId(), p.getCode(), p.getName(), p.getSpecies(), p.getSex(),
                p.getEstimatedBirthDate(), p.getColor(), p.getDescription(), p.isSterilized(), photoIds);
        }
    }

    @GetMapping
    public PageResponse<PublicPet> list(@RequestParam(required = false) Species species,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "12") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Pet> result = species == null
            ? pets.findByStatus(PetStatus.AVAILABLE, pageable)
            : pets.findByStatusAndSpecies(PetStatus.AVAILABLE, species, pageable);
        return PageResponse.of(result, this::toPublic);
    }

    @GetMapping("/{id}")
    public PublicPet detail(@PathVariable Long id) {
        return pets.findByIdAndStatus(id, PetStatus.AVAILABLE).map(this::toPublic)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy thú hoặc thú không còn mở nhận nuôi"));
    }
}
