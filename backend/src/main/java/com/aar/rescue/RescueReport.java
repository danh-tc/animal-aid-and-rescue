package com.aar.rescue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Báo cáo cứu hộ của Khách. Lưu trực tiếp tên và số điện thoại, không gắn tài khoản. */
@Entity
@Table(name = "rescue_reports")
public class RescueReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "reporter_name", nullable = false)
    private String reporterName;

    @Column(name = "reporter_phone", nullable = false)
    private String reporterPhone;

    @Column(name = "location_text", nullable = false)
    private String locationText;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "handled_at")
    private OffsetDateTime handledAt;

    protected RescueReport() {
    }

    public RescueReport(String code, String reporterName, String reporterPhone, String locationText,
                        BigDecimal latitude, BigDecimal longitude, String description, ReportStatus status) {
        this.code = code;
        this.reporterName = reporterName;
        this.reporterPhone = reporterPhone;
        this.locationText = locationText;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }

    /** Chỉ gọi với trạng thái đích do StateTransitionService trả về. */
    public void changeStatus(ReportStatus status, Long handledBy) {
        this.status = status;
        this.handledBy = handledBy;
        this.handledAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getReporterName() { return reporterName; }
    public String getReporterPhone() { return reporterPhone; }
    public String getLocationText() { return locationText; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getDescription() { return description; }
    public ReportStatus getStatus() { return status; }
    public Long getHandledBy() { return handledBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getHandledAt() { return handledAt; }
}
