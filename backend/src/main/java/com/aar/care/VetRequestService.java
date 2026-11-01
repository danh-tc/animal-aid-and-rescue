package com.aar.care;

import com.aar.common.storage.AttachmentOwner;
import com.aar.common.storage.StorageService;
import org.springframework.web.multipart.MultipartFile;

import com.aar.adoption.ApplicationCascade;
import com.aar.care.CareDtos.VetRequestComplete;
import com.aar.care.CareDtos.VetRequestCreate;
import com.aar.care.CareDtos.VetRequestResult;
import com.aar.care.CareDtos.VetRequestView;
import com.aar.common.Pages;
import com.aar.common.PageResponse;
import com.aar.common.error.BusinessException;
import com.aar.common.state.EntityType;
import com.aar.common.state.StateTransitionService;
import com.aar.pet.Pet;
import com.aar.pet.PetLifecycle;
import com.aar.pet.PetStatus;
import com.aar.system.VetClinic;
import com.aar.system.VetClinicRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** S06: yêu cầu thú y. Thú ở Đang điều trị khi còn ít nhất một yêu cầu mở (QĐ-06). */
@Service
@Transactional
public class VetRequestService {

    private final VetRequestRepository requests;
    private final VetClinicRepository clinics;
    private final StateTransitionService state;
    private final PetLifecycle petLifecycle;
    private final ApplicationCascade applicationCascade;
    private final StorageService storage;

    public VetRequestService(VetRequestRepository requests, VetClinicRepository clinics, StateTransitionService state,
                             PetLifecycle petLifecycle, ApplicationCascade applicationCascade, StorageService storage) {
        this.requests = requests;
        this.clinics = clinics;
        this.state = state;
        this.petLifecycle = petLifecycle;
        this.applicationCascade = applicationCascade;
        this.storage = storage;
    }

    /** S-2 của UC-09 và ngoại lệ 3a: thú Sẵn sàng hoặc Chờ bàn giao bị bệnh thì hủy đơn liên quan. */
    public VetRequestResult create(Long petId, VetRequestCreate req, Long by) {
        Pet pet = petLifecycle.lock(petId);
        if (!pet.getStatus().atCenter()) {
            throw BusinessException.conflict("PET_NOT_AT_CENTER",
                "Chỉ tạo yêu cầu thú y cho thú đang ở trung tâm (trạng thái hiện tại: " + pet.getStatus() + ")");
        }
        VetClinic clinic = clinics.findById(req.clinicId()).filter(VetClinic::isActive)
            .orElseThrow(() -> BusinessException.badRequest("INVALID_CLINIC", "Cơ sở thú y không tồn tại hoặc đã ngừng hợp tác"));

        VetRequestStatus initial = VetRequestStatus.valueOf(state.initialStatus(EntityType.VET_REQUEST, "CREATED"));
        VetRequest request = requests.save(new VetRequest(pet.getId(), clinic.getId(), req.reason().trim(),
            req.priority(), req.careMode(), initial, by));
        state.apply(EntityType.VET_REQUEST, request.getId(), null, "CREATED", by, clinic.getName());

        int cancelled = 0;
        if (pet.getStatus() != PetStatus.IN_TREATMENT) {
            if (pet.getStatus() == PetStatus.AVAILABLE || pet.getStatus() == PetStatus.PENDING_HANDOVER) {
                cancelled = applicationCascade.cancelOpen(pet.getId(), ApplicationCascade.OPEN,
                    "Hệ thống tự đóng: thú cần điều trị", by);
            }
            petLifecycle.transition(pet, "VET_REQUEST_OPENED", by, "Yêu cầu thú y #" + request.getId());
        }
        if (req.careMode() == CareMode.INPATIENT) {
            pet.changeLocation(clinic.getName());
        }
        return new VetRequestResult(view(request, clinic), pet.getStatus(), cancelled);
    }

    public VetRequestResult start(Long requestId, Long by) {
        VetRequest request = find(requestId);
        transition(request, "STARTED", by, null);
        return new VetRequestResult(view(request), petLifecycle.lock(request.getPetId()).getStatus(), 0);
    }

    /** S-3 của UC-09: ghi kết quả. Không còn yêu cầu mở thì thú về Đang chăm sóc. */
    public VetRequestResult complete(Long requestId, VetRequestComplete req, Long by) {
        return complete(requestId, req, List.of(), by);
    }

    /** Bản multipart theo mục 5.2: kết quả, chi phí, hướng dẫn và phiếu khám trong cùng một giao dịch. */
    public VetRequestResult complete(Long requestId, VetRequestComplete req, List<MultipartFile> files, Long by) {
        VetRequest request = find(requestId);
        Pet pet = petLifecycle.lock(request.getPetId());
        // Ghi kết quả trước khi đổi trạng thái: CHECK ck_vet_completed_cost đòi COMPLETED phải có chi phí (RB-07)
        request.recordResult(blankToNull(req.diagnosisSummary()), blankToNull(req.treatmentSummary()),
            req.totalCost(), blankToNull(req.aftercareInstructions()), req.healthOk());
        transition(request, "COMPLETED", by, null);
        storage.store(AttachmentOwner.VET_REQUEST, request.getId(), files);
        closeTreatmentIfDone(pet, by);
        return new VetRequestResult(view(request), pet.getStatus(), 0);
    }

    /** Ngoại lệ S-2a của UC-09: phòng khám không nhận điều trị. */
    public VetRequestResult cancel(Long requestId, String reason, Long by) {
        VetRequest request = find(requestId);
        Pet pet = petLifecycle.lock(request.getPetId());
        request.recordCancel(reason.trim());
        transition(request, "CANCELLED", by, reason);
        closeTreatmentIfDone(pet, by);
        return new VetRequestResult(view(request), pet.getStatus(), 0);
    }

    /**
     * QĐ-15: sự kiện kết thúc hủy mọi yêu cầu thú y đang mở của thú. Không đổi trạng thái thú,
     * người gọi chịu trách nhiệm chuyển thú sang trạng thái kết thúc. Trả về số yêu cầu đã hủy.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public int cancelAllOpen(Long petId, String reason, Long by) {
        List<VetRequest> open = requests.findByPetIdAndStatusIn(petId, VetRequestStatus.OPEN);
        for (VetRequest r : open) {
            r.recordCancel(reason);
            transition(r, "CANCELLED", by, reason);
        }
        return open.size();
    }

    @Transactional(readOnly = true)
    public List<VetRequestView> listForPet(Long petId) {
        return views(requests.findByPetIdOrderByRequestedAtDesc(petId));
    }

    @Transactional(readOnly = true)
    public PageResponse<VetRequestView> list(VetRequestStatus status, int page, int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        var statuses = status == null ? EnumSet.allOf(VetRequestStatus.class) : EnumSet.of(status);
        var result = requests.findByStatusIn(statuses, pageable);
        Map<Long, VetClinic> byId = clinicsById();
        return PageResponse.of(result, r -> view(r, byId.get(r.getClinicId())));
    }

    @Transactional(readOnly = true)
    public VetRequestView detail(Long id) {
        return view(find(id));
    }

    private void closeTreatmentIfDone(Pet pet, Long by) {
        if (pet.getStatus() == PetStatus.IN_TREATMENT
            && requests.countByPetIdAndStatusIn(pet.getId(), VetRequestStatus.OPEN) == 0) {
            petLifecycle.transition(pet, "VET_REQUEST_CLOSED", by, null);
            pet.changeLocation(PetLifecycle.CENTER_LOCATION);
        }
    }

    private void transition(VetRequest request, String event, Long by, String note) {
        String to = state.apply(EntityType.VET_REQUEST, request.getId(), request.getStatus().name(), event, by, note);
        request.changeStatus(VetRequestStatus.valueOf(to));
    }

    private VetRequest find(Long id) {
        return requests.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy yêu cầu thú y"));
    }

    List<VetRequestView> views(List<VetRequest> list) {
        Map<Long, VetClinic> byId = clinicsById();
        return list.stream().map(r -> view(r, byId.get(r.getClinicId()))).toList();
    }

    private Map<Long, VetClinic> clinicsById() {
        return clinics.findAll().stream().collect(Collectors.toMap(VetClinic::getId, Function.identity()));
    }

    private VetRequestView view(VetRequest r) {
        return view(r, clinics.findById(r.getClinicId()).orElse(null));
    }

    private static VetRequestView view(VetRequest r, VetClinic clinic) {
        return new VetRequestView(r.getId(), r.getPetId(), r.getClinicId(), clinic == null ? null : clinic.getName(),
            r.getReason(), r.getPriority(), r.getCareMode(), r.getStatus(), r.getDiagnosisSummary(),
            r.getTreatmentSummary(), r.getTotalCost(), r.getAftercareInstructions(), r.getHealthOk(), r.getCancelReason(),
            r.getRequestedAt(), r.getCompletedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
