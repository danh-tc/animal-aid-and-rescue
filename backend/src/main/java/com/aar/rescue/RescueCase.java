package com.aar.rescue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_text", nullable = false)
    private String locationText;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @ManyToMany
    @JoinTable(name = "report_cases",
        joinColumns = @JoinColumn(name = "case_id"),
        inverseJoinColumns = @JoinColumn(name = "report_id"))
    private Set<RescueReport> reports = new LinkedHashSet<>();

    protected RescueCase() {
    }

    public RescueCase(RescueReport report, CaseStatus status, Long createdBy) {
        this.locationText = report.getLocationText();
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
        this.reports.add(report);
    }

    public void changeStatus(CaseStatus status) {
        this.status = status;
        if (status == CaseStatus.SUCCEEDED || status == CaseStatus.FAILED) {
            this.closedAt = OffsetDateTime.now();
        }
    }

    public void addReport(RescueReport report) {
        this.reports.add(report);
    }

    public void assignTo(Long staffId) {
        this.assignedStaffId = staffId;
    }

    public void recordFailure(String reason) {
        this.failureReason = reason;
    }

    public Long getId() { return id; }
    public String getLocationText() { return locationText; }
    public Long getAssignedStaffId() { return assignedStaffId; }
    public CaseStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Long getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public Set<RescueReport> getReports() { return reports; }
}
