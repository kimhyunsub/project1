package com.attendance.backend.domain.entity;

import com.attendance.backend.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_requests")
public class WorkRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HalfDayType halfDayType;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private SpecialLeaveOccasionType occasionType;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column
    private Integer earlyLeaveMinutes;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_employee_id")
    private Employee reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String reviewNote;

    private LocalDateTime canceledAt;

    protected WorkRequest() {
    }

    public WorkRequest(
        Employee employee,
        Company company,
        WorkRequestType requestType,
        WorkRequestStatus status,
        HalfDayType halfDayType,
        SpecialLeaveOccasionType occasionType,
        LocalDate requestDate,
        Integer earlyLeaveMinutes,
        String reason
    ) {
        this.employee = employee;
        this.company = company;
        this.requestType = requestType;
        this.status = status;
        this.halfDayType = halfDayType;
        this.occasionType = occasionType;
        this.requestDate = requestDate;
        this.earlyLeaveMinutes = earlyLeaveMinutes;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Company getCompany() {
        return company;
    }

    public WorkRequestType getRequestType() {
        return requestType;
    }

    public WorkRequestStatus getStatus() {
        return status;
    }

    public HalfDayType getHalfDayType() {
        return halfDayType;
    }

    public SpecialLeaveOccasionType getOccasionType() {
        return occasionType;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public Integer getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }

    public String getReason() {
        return reason;
    }

    public Employee getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public boolean isPending() {
        return status == WorkRequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == WorkRequestStatus.APPROVED;
    }

    public void approve(Employee reviewer, String reviewNote) {
        this.status = WorkRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = reviewNote;
        this.canceledAt = null;
    }

    public void reject(Employee reviewer, String reviewNote) {
        this.status = WorkRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = reviewNote;
        this.canceledAt = null;
    }

    public void cancel() {
        this.status = WorkRequestStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void requestCancellation() {
        this.status = WorkRequestStatus.CANCEL_REQUESTED;
    }

    public void cancel(Employee reviewer, String reviewNote) {
        this.status = WorkRequestStatus.CANCELED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = reviewNote;
        this.canceledAt = LocalDateTime.now();
    }
}
