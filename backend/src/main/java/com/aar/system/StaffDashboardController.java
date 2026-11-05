package com.aar.system;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Màn hình S01: bảng công việc của Nhân viên (Quản lý cũng dùng được nhờ RoleHierarchy). */
@RestController
public class StaffDashboardController {

    private final JdbcTemplate jdbc;

    public StaffDashboardController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** overdueUnassignedCases: ca ở Mới quá CASE_UNASSIGNED_ALERT_HOURS giờ (QĐ-17). recallPending: bàn giao chờ thu hồi. */
    public record Dashboard(long newReports, long openCases, long pendingApplications,
                            long followUpsDueThisWeek, long openVetRequests, long overdueUnassignedCases,
                            long recallPending) {
    }

    @GetMapping("/api/staff/dashboard")
    public Dashboard dashboard() {
        return new Dashboard(
            count("SELECT count(*) FROM rescue_reports WHERE status = 'NEW'"),
            count("SELECT count(*) FROM rescue_cases WHERE status IN ('NEW','IN_PROGRESS')"),
            count("SELECT count(*) FROM adoption_applications WHERE status = 'PENDING'"),
            count("SELECT count(*) FROM follow_ups WHERE status = 'SCHEDULED' AND due_date <= current_date + 7"),
            count("SELECT count(*) FROM vet_requests WHERE status IN ('PENDING','IN_TREATMENT')"),
            count("""
                SELECT count(*) FROM rescue_cases
                WHERE status = 'NEW' AND created_at < now() - make_interval(hours =>
                    (SELECT param_value::int FROM system_parameters WHERE param_key = 'CASE_UNASSIGNED_ALERT_HOURS'))"""),
            count("SELECT count(*) FROM handovers WHERE follow_up_status = 'RECALL_PENDING'"));
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
