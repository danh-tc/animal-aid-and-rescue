package com.aar.report;

import com.aar.common.error.BusinessException;
import com.aar.report.ReportDtos.ClinicCost;
import com.aar.report.ReportDtos.CostReport;
import com.aar.report.ReportDtos.ExceptionReport;
import com.aar.report.ReportDtos.MonthlyCost;
import com.aar.report.ReportDtos.MonthlySummary;
import com.aar.report.ReportDtos.OverdueFollowUp;
import com.aar.report.ReportDtos.Period;
import com.aar.report.ReportDtos.PetCost;
import com.aar.report.ReportDtos.RecallPending;
import com.aar.report.ReportDtos.StaleApplication;
import com.aar.report.ReportDtos.SummaryReport;
import com.aar.report.ReportDtos.UnassignedCase;
import com.aar.system.SystemParameterService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_MONTHS = 36;

    private final NamedParameterJdbcTemplate jdbc;
    private final SystemParameterService params;

    public ReportService(NamedParameterJdbcTemplate jdbc, SystemParameterService params) {
        this.jdbc = jdbc;
        this.params = params;
    }

    public static Period period(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(ZONE) : to;
        LocalDate start = from == null ? YearMonth.from(end).minusMonths(11).atDay(1) : from;
        if (start.isAfter(end)) {
            throw BusinessException.badRequest("INVALID_PERIOD", "Ngày bắt đầu phải trước ngày kết thúc");
        }
        if (ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) >= MAX_MONTHS) {
            throw BusinessException.badRequest("INVALID_PERIOD", "Kỳ báo cáo tối đa " + MAX_MONTHS + " tháng");
        }
        return new Period(start, end);
    }

    public SummaryReport summary(Period p) {
        MapSqlParameterSource q = range(p);
        long reports = count("SELECT count(*) FROM rescue_reports WHERE created_at >= :from AND created_at < :to", q);
        long created = count("SELECT count(*) FROM rescue_cases WHERE created_at >= :from AND created_at < :to", q);
        long succeeded = count("SELECT count(*) FROM rescue_cases WHERE status = 'SUCCEEDED' AND closed_at >= :from AND closed_at < :to", q);
        long failed = count("SELECT count(*) FROM rescue_cases WHERE status = 'FAILED' AND closed_at >= :from AND closed_at < :to", q);
        long adoptions = count("SELECT count(*) FROM handovers WHERE handed_over_at >= :from AND handed_over_at < :to", q);
        long returns = count("SELECT count(*) FROM intakes WHERE source = 'RETURN' AND intake_at >= :from AND intake_at < :to", q);

        Map<String, Long> endEvents = new LinkedHashMap<>();
        for (String type : List.of("DECEASED", "LOST")) {
            endEvents.put(type, 0L);
        }
        jdbc.query("SELECT event_type, count(*) n FROM pet_end_events WHERE occurred_at >= :from AND occurred_at < :to GROUP BY event_type",
            q, rs -> { endEvents.put(rs.getString("event_type"), rs.getLong("n")); });

        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbc.query("SELECT status, count(*) n FROM pets GROUP BY status ORDER BY status", q,
            rs -> { byStatus.put(rs.getString("status"), rs.getLong("n")); });

        Map<String, long[]> months = emptyMonths(p, 3);
        monthly("SELECT to_char(closed_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') m, count(*) n FROM rescue_cases "
            + "WHERE status = 'SUCCEEDED' AND closed_at >= :from AND closed_at < :to GROUP BY m", q, months, 0);
        monthly("SELECT to_char(handed_over_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') m, count(*) n FROM handovers "
            + "WHERE handed_over_at >= :from AND handed_over_at < :to GROUP BY m", q, months, 1);
        monthly("SELECT to_char(intake_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') m, count(*) n FROM intakes "
            + "WHERE source = 'RETURN' AND intake_at >= :from AND intake_at < :to GROUP BY m", q, months, 2);
        List<MonthlySummary> rows = months.entrySet().stream()
            .map(e -> new MonthlySummary(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();

        return new SummaryReport(p, reports, created, succeeded, failed, adoptions, returns, endEvents, byStatus, rows);
    }

    public ExceptionReport exceptions() {
        int alertDays = params.getInt(SystemParameterService.APPLICATION_ALERT_DAYS);
        LocalDate today = LocalDate.now(ZONE);
        MapSqlParameterSource q = new MapSqlParameterSource()
            .addValue("today", today)
            .addValue("cutoff", Timestamp.from(OffsetDateTime.now().minusDays(alertDays).toInstant()));

        List<OverdueFollowUp> overdue = jdbc.query("""
            SELECT f.id, p.id pet_id, p.code, p.name, f.sequence_no, f.due_date, u.full_name, u.phone
            FROM follow_ups f
            JOIN handovers h ON h.id = f.handover_id
            JOIN pets p ON p.id = h.pet_id
            JOIN adoption_applications a ON a.id = h.application_id
            JOIN users u ON u.id = a.applicant_id
            WHERE f.status = 'SCHEDULED' AND f.due_date < :today
            ORDER BY f.due_date, f.id""", q, (rs, i) -> {
                LocalDate due = rs.getObject("due_date", LocalDate.class);
                return new OverdueFollowUp(rs.getLong("id"), rs.getLong("pet_id"), rs.getString("code"),
                    rs.getString("name"), rs.getInt("sequence_no"), due, ChronoUnit.DAYS.between(due, today),
                    rs.getString("full_name"), rs.getString("phone"));
            });

        List<StaleApplication> stale = jdbc.query("""
            SELECT a.id, p.id pet_id, p.code, p.name, u.full_name, a.submitted_at
            FROM adoption_applications a
            JOIN pets p ON p.id = a.pet_id
            JOIN users u ON u.id = a.applicant_id
            WHERE a.status = 'PENDING' AND a.submitted_at < :cutoff
            ORDER BY a.submitted_at, a.id""", q, (rs, i) -> {
                OffsetDateTime submitted = rs.getObject("submitted_at", OffsetDateTime.class);
                return new StaleApplication(rs.getLong("id"), rs.getLong("pet_id"), rs.getString("code"),
                    rs.getString("name"), rs.getString("full_name"), submitted,
                    ChronoUnit.DAYS.between(submitted.atZoneSameInstant(ZONE).toLocalDate(), today));
            });

        List<RecallPending> recall = recallPending();

        int unassignedHours = params.getInt(SystemParameterService.CASE_UNASSIGNED_ALERT_HOURS);
        OffsetDateTime now = OffsetDateTime.now();
        q.addValue("caseCutoff", Timestamp.from(now.minusHours(unassignedHours).toInstant()));
        List<UnassignedCase> unassigned = jdbc.query("""
            SELECT c.id, c.location_text, c.created_at
            FROM rescue_cases c
            WHERE c.status = 'NEW' AND c.created_at < :caseCutoff
            ORDER BY c.created_at, c.id""", q, (rs, i) -> {
                OffsetDateTime created = rs.getObject("created_at", OffsetDateTime.class);
                return new UnassignedCase(rs.getLong("id"), rs.getString("location_text"), created,
                    ChronoUnit.HOURS.between(created, now));
            });

        return new ExceptionReport(alertDays, overdue, stale, recall, unassignedHours, unassigned);
    }

    public List<RecallPending> recallPending() {
        int recallDays = params.getInt(SystemParameterService.RECALL_CLOSE_AFTER_DAYS);
        return jdbc.query("""
            SELECT h.id, p.id pet_id, p.code, p.name, u.full_name, u.phone, h.handed_over_at,
                   (SELECT max(f.performed_at) FROM follow_ups f WHERE f.handover_id = h.id AND f.result = 'FAIL') failed_at
            FROM handovers h
            JOIN pets p ON p.id = h.pet_id
            JOIN adoption_applications a ON a.id = h.application_id
            JOIN users u ON u.id = a.applicant_id
            WHERE h.follow_up_status = 'RECALL_PENDING'
            ORDER BY h.handed_over_at""", new MapSqlParameterSource(), (rs, i) -> {
                OffsetDateTime failedAt = rs.getObject("failed_at", OffsetDateTime.class);
                return new RecallPending(rs.getLong("id"), rs.getLong("pet_id"),
                    rs.getString("code"), rs.getString("name"), rs.getString("full_name"), rs.getString("phone"),
                    rs.getObject("handed_over_at", OffsetDateTime.class), failedAt,
                    failedAt == null ? null : failedAt.plusDays(recallDays));
            });
    }

    public CostReport costs(Period p) {
        MapSqlParameterSource q = range(p);
        Map<String, BigDecimal[]> months = new LinkedHashMap<>();
        for (String m : emptyMonths(p, 0).keySet()) {
            months.put(m, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        }
        jdbc.query("SELECT to_char(completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') m, SUM(total_cost) s "
            + "FROM vet_requests WHERE status IN ('COMPLETED','CANCELLED') AND completed_at >= :from AND completed_at < :to GROUP BY m",
            q, rs -> { addMonth(months, rs.getString("m"), 0, rs.getBigDecimal("s")); });
        jdbc.query("SELECT to_char(handed_over_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') m, SUM(donation_amount) s "
            + "FROM handovers WHERE handed_over_at >= :from AND handed_over_at < :to GROUP BY m",
            q, rs -> { addMonth(months, rs.getString("m"), 1, rs.getBigDecimal("s")); });
        List<MonthlyCost> monthRows = months.entrySet().stream().map(e -> new MonthlyCost(e.getKey(),
            e.getValue()[0], e.getValue()[1], e.getValue()[0].subtract(e.getValue()[1]))).toList();
        BigDecimal totalCost = monthRows.stream().map(MonthlyCost::treatmentCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDonations = monthRows.stream().map(MonthlyCost::donations).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ClinicCost> byClinic = jdbc.query("""
            SELECT c.id, c.name, count(v.id) n, COALESCE(SUM(v.total_cost), 0) s
            FROM vet_clinics c
            JOIN vet_requests v ON v.clinic_id = c.id AND v.status IN ('COMPLETED','CANCELLED')
                AND v.completed_at >= :from AND v.completed_at < :to
            GROUP BY c.id, c.name
            ORDER BY s DESC, c.name""", q, (rs, i) -> new ClinicCost(rs.getLong("id"), rs.getString("name"),
                rs.getLong("n"), rs.getBigDecimal("s")));

        List<PetCost> byPet = jdbc.query("""
            SELECT p.id, p.code, p.name, p.status,
                   COALESCE((SELECT SUM(total_cost) FROM vet_requests v WHERE v.pet_id = p.id AND v.status IN ('COMPLETED','CANCELLED')), 0) cost,
                   COALESCE((SELECT SUM(donation_amount) FROM handovers h WHERE h.pet_id = p.id), 0) donated
            FROM pets p
            WHERE EXISTS (SELECT 1 FROM vet_requests v WHERE v.pet_id = p.id AND v.status IN ('COMPLETED','CANCELLED'))
               OR EXISTS (SELECT 1 FROM handovers h WHERE h.pet_id = p.id)
            ORDER BY cost DESC, p.code""", q, (rs, i) -> {
                BigDecimal cost = rs.getBigDecimal("cost");
                BigDecimal donated = rs.getBigDecimal("donated");
                return new PetCost(rs.getLong("id"), rs.getString("code"), rs.getString("name"), rs.getString("status"),
                    cost, donated, cost.subtract(donated).max(BigDecimal.ZERO));
            });

        return new CostReport(p, totalCost, totalDonations, monthRows, byClinic, byPet);
    }

    private static MapSqlParameterSource range(Period p) {
        return new MapSqlParameterSource()
            .addValue("from", Timestamp.from(p.from().atStartOfDay(ZONE).toInstant()))
            .addValue("to", Timestamp.from(p.to().plusDays(1).atStartOfDay(ZONE).toInstant()));
    }

    private long count(String sql, MapSqlParameterSource q) {
        Long n = jdbc.queryForObject(sql, q, Long.class);
        return n == null ? 0 : n;
    }

    private static Map<String, long[]> emptyMonths(Period p, int width) {
        Map<String, long[]> months = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(p.from()); !m.isAfter(YearMonth.from(p.to())); m = m.plusMonths(1)) {
            months.put(m.toString(), new long[width]);
        }
        return months;
    }

    private void monthly(String sql, MapSqlParameterSource q, Map<String, long[]> months, int column) {
        jdbc.query(sql, q, rs -> {
            long[] row = months.get(rs.getString("m"));
            if (row != null) {
                row[column] = rs.getLong("n");
            }
        });
    }

    private static void addMonth(Map<String, BigDecimal[]> months, String month, int column, BigDecimal value) {
        BigDecimal[] row = months.get(month);
        if (row != null && value != null) {
            row[column] = row[column].add(value);
        }
    }
}
