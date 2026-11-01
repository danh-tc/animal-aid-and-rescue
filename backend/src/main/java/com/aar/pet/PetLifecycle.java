package com.aar.pet;

import com.aar.common.code.CodeGenerator;
import com.aar.common.error.BusinessException;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Thao tác dùng chung trên hồ sơ thú: tải có khóa dòng, tạo hồ sơ, đổi trạng thái qua RB-12. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PetLifecycle {

    public static final String CENTER_LOCATION = "Trung tâm";
    public static final String OWNER_LOCATION = "Nhà chủ nuôi";

    private final PetRepository pets;
    private final StateTransitionService state;
    private final CodeGenerator codes;

    public PetLifecycle(PetRepository pets, StateTransitionService state, CodeGenerator codes) {
        this.pets = pets;
        this.state = state;
        this.codes = codes;
    }

    public Pet lock(Long petId) {
        return pets.findByIdForUpdate(petId).orElseThrow(() -> BusinessException.notFound("Không tìm thấy thú"));
    }

    /** Hồ sơ mới sau ca cứu hộ thành công: IN_CARE, khu cách ly (bảng 3.4). */
    public Pet createFromRescue(String name, Species species, Sex sex, LocalDate birth, String color,
                                String description, Long by) {
        PetStatus initial = PetStatus.valueOf(state.initialStatus(EntityType.PET, "RESCUE_SUCCEEDED"));
        Pet pet = pets.save(new Pet(codes.nextPetCode(), name, species, sex == null ? Sex.UNKNOWN : sex, birth,
            color, description, initial, PetArea.QUARANTINE, CENTER_LOCATION));
        state.apply(EntityType.PET, pet.getId(), null, "RESCUE_SUCCEEDED", by, null);
        return pet;
    }

    public void transition(Pet pet, String event, Long by, String note) {
        String to = state.apply(EntityType.PET, pet.getId(), pet.getStatus().name(), event, by, note);
        pet.changeStatus(PetStatus.valueOf(to));
    }
}
