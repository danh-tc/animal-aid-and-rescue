package com.aar.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record Period(LocalDate from, LocalDate to) {
    }

    public record SummaryReport(Period period, long reportsReceived, long casesCreated, long casesSucceeded,
                                long casesFailed, long adoptions, long returns, Map<String, Long> endEvents,
                                Map<String, Long> petsByStatus, List<MonthlySummary> months) {
    }

    public record MonthlySummary(String month, long casesSucceeded, long adoptions, long returns) {
    }

    public record ExceptionReport(int applicationAlertDays, List<OverdueFollowUp> overdueFollowUps,
                                  List<StaleApplication> staleApplications, List<RecallPending> recallPending,
                                  int caseUnassignedAlertHours, List<UnassignedCase> unassignedCases) {
    }

    public record UnassignedCase(Long caseId, String locationText, OffsetDateTime createdAt, long hoursWaiting) {
    }

    public record OverdueFollowUp(Long followUpId, Long petId, String petCode, String petName, int sequenceNo,
                                  LocalDate dueDate, long daysOverdue, String ownerName, String ownerPhone) {
    }

    public record StaleApplication(Long applicationId, Long petId, String petCode, String petName,
                                   String applicantName, OffsetDateTime submittedAt, long daysWaiting) {
    }

    public record RecallPending(Long handoverId, Long petId, String petCode, String petName, String ownerName,
                                String ownerPhone, OffsetDateTime handedOverAt, OffsetDateTime failedAt,
                                OffsetDateTime closableFrom) {
    }

    public record CostReport(Period period, BigDecimal periodTreatmentCost, BigDecimal periodDonations,
                             List<MonthlyCost> months, List<ClinicCost> byClinic, List<PetCost> byPet) {
    }

    public record MonthlyCost(String month, BigDecimal treatmentCost, BigDecimal donations, BigDecimal difference) {
    }

    public record ClinicCost(Long clinicId, String clinicName, long completedRequests, BigDecimal treatmentCost) {
    }

    public record PetCost(Long petId, String petCode, String petName, String petStatus, BigDecimal treatmentCost,
                          BigDecimal donated, BigDecimal outstanding) {
    }
}
