package com.aar.handover;

import com.aar.common.error.BusinessException;
import com.aar.handover.HandoverDtos.PetCosts;
import com.aar.pet.PetRepository;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QĐ-14: tổng chi phí điều trị (mọi yêu cầu Hoàn tất, kể cả sau khi bị trả lại), đã được ủng hộ
 * (cộng dồn qua các lần bàn giao), còn chưa bù đắp = tổng trừ ủng hộ, thấp nhất là 0.
 */
@Service
@Transactional(readOnly = true)
public class PetCostService {

    private final JdbcTemplate jdbc;
    private final PetRepository pets;

    public PetCostService(JdbcTemplate jdbc, PetRepository pets) {
        this.jdbc = jdbc;
        this.pets = pets;
    }

    public PetCosts costs(Long petId) {
        if (!pets.existsById(petId)) {
            throw BusinessException.notFound("Không tìm thấy thú");
        }
        BigDecimal cost = sum("SELECT COALESCE(SUM(total_cost), 0) FROM vet_requests WHERE pet_id = ? AND status IN ('COMPLETED','CANCELLED')", petId);
        BigDecimal donated = sum("SELECT COALESCE(SUM(donation_amount), 0) FROM handovers WHERE pet_id = ?", petId);
        return new PetCosts(petId, cost, donated, cost.subtract(donated).max(BigDecimal.ZERO));
    }

    private BigDecimal sum(String sql, Long petId) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, petId);
        return v == null ? BigDecimal.ZERO : v;
    }
}
