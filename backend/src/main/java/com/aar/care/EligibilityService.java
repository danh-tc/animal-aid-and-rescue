package com.aar.care;

import com.aar.pet.Intake;
import com.aar.pet.IntakeRepository;
import com.aar.pet.Pet;
import com.aar.system.SystemParameterService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checklist 6 mục đủ điều kiện nhận nuôi (báo cáo 2.1.3, QĐ-07, RB-13).
 * Mục 2, 3 và tính cách do nhân viên đánh dấu. Mục 1, 4, 5, 6 tính từ dữ liệu.
 */
@Service
@Transactional(readOnly = true)
public class EligibilityService {

    public record Item(String key, String label, boolean passed, String detail) {
    }

    public record Checklist(boolean allPassed, List<Item> items) {

        public List<String> missing() {
            return items.stream().filter(i -> !i.passed()).map(Item::label).toList();
        }
    }

    private final VetRequestRepository vetRequests;
    private final IntakeRepository intakes;
    private final SystemParameterService params;

    public EligibilityService(VetRequestRepository vetRequests, IntakeRepository intakes,
                              SystemParameterService params) {
        this.vetRequests = vetRequests;
        this.intakes = intakes;
        this.params = params;
    }

    public Checklist evaluate(Pet pet) {
        long open = vetRequests.countByPetIdAndStatusIn(pet.getId(), VetRequestStatus.OPEN);
        OffsetDateTime lastIntake = intakes.findFirstByPetIdOrderByIntakeAtDesc(pet.getId())
            .map(Intake::getIntakeAt).orElse(null);
        // Checklist mục 1 (QĐ-05): không còn yêu cầu mở và lần khám gần nhất TRONG lượt tiếp nhận hiện tại kết luận ổn.
        // Thú bị trả lại hoặc tìm lại phải khám lại, kết quả khám của lượt trước không được tính.
        var lastExam = vetRequests.findFirstByPetIdAndStatusOrderByCompletedAtDesc(pet.getId(), VetRequestStatus.COMPLETED)
            .filter(v -> lastIntake == null || (v.getCompletedAt() != null && v.getCompletedAt().isAfter(lastIntake)));
        boolean lastOk = lastExam.map(v -> Boolean.TRUE.equals(v.getHealthOk())).orElse(false);
        boolean health = open == 0 && lastOk;
        String healthDetail = open > 0 ? "Còn " + open + " yêu cầu thú y đang mở"
            : lastExam.isEmpty() ? "Chưa khám thú y trong lượt tiếp nhận hiện tại (QĐ-05)"
            : lastOk ? null : "Lần khám gần nhất kết luận sức khỏe chưa ổn";

        Long ageWeeks = pet.getEstimatedBirthDate() == null ? null
            : ChronoUnit.WEEKS.between(pet.getEstimatedBirthDate(), LocalDate.now());
        int minAdoption = params.getInt(SystemParameterService.MIN_ADOPTION_AGE_WEEKS);
        boolean ageOk = ageWeeks != null && ageWeeks >= minAdoption;
        String ageDetail = ageWeeks == null ? "Chưa có ngày sinh ước tính"
            : ageOk ? null : "Mới " + ageWeeks + " tuần, cần tối thiểu " + minAdoption + " tuần";

        int minSterilization = params.getInt(SystemParameterService.MIN_STERILIZATION_AGE_WEEKS);
        boolean tooYoungToSterilize = ageWeeks != null && ageWeeks < minSterilization;
        boolean sterilizationOk = pet.isSterilized() || tooYoungToSterilize;
        String sterilizationDetail = pet.isSterilized() ? null
            : tooYoungToSterilize ? "Chưa đủ tuổi triệt sản nên mục này đạt"
            : "Đủ tuổi nhưng chưa triệt sản";

        int quarantineDays = params.getInt(SystemParameterService.QUARANTINE_DAYS);
        boolean quarantineOver = lastIntake != null && !lastIntake.plusDays(quarantineDays).isAfter(OffsetDateTime.now());
        boolean observationOk = quarantineOver && pet.isTemperamentOk();
        String observationDetail = !quarantineOver
            ? "Chưa hết " + quarantineDays + " ngày cách ly sau lượt tiếp nhận gần nhất"
            : pet.isTemperamentOk() ? null : "Chưa xác nhận tính cách không hung dữ";

        List<Item> items = List.of(
            new Item("HEALTH", "Sức khỏe ổn định", health, healthDetail),
            new Item("VACCINATED", "Đã tiêm phòng cơ bản", pet.isVaccinated(), null),
            new Item("DEWORMED", "Đã tẩy giun, ký sinh trùng", pet.isDewormed(), null),
            new Item("AGE", "Đủ tuổi tách mẹ", ageOk, ageDetail),
            new Item("STERILIZATION", "Triệt sản hoặc chưa đủ tuổi triệt sản", sterilizationOk, sterilizationDetail),
            new Item("OBSERVATION", "Hết cách ly, tính cách không hung dữ", observationOk, observationDetail));
        return new Checklist(items.stream().allMatch(Item::passed), items);
    }
}
