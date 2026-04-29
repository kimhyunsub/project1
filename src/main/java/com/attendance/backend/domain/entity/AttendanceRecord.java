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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendance_records",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_employee_date", columnNames = {"employee_id", "attendance_date"})
    }
)
public class AttendanceRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    @Column(nullable = false)
    private Double checkInLatitude;

    @Column(nullable = false)
    private Double checkInLongitude;

    private Double checkOutLatitude;

    private Double checkOutLongitude;

    @Column(nullable = false)
    private boolean late;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(
        Employee employee,
        LocalDate attendanceDate,
        LocalDateTime checkInTime,
        Double checkInLatitude,
        Double checkInLongitude,
        boolean late,
        AttendanceStatus status
    ) {
        this.employee = employee;
        this.attendanceDate = attendanceDate;
        this.checkInTime = checkInTime;
        this.checkInLatitude = checkInLatitude;
        this.checkInLongitude = checkInLongitude;
        this.late = late;
        this.status = status;
    }

    public void checkOut(LocalDateTime checkOutTime, Double checkOutLatitude, Double checkOutLongitude) {
        this.checkOutTime = checkOutTime;
        this.checkOutLatitude = checkOutLatitude;
        this.checkOutLongitude = checkOutLongitude;
        this.status = AttendanceStatus.CHECKED_OUT;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public Double getCheckInLatitude() {
        return checkInLatitude;
    }

    public Double getCheckInLongitude() {
        return checkInLongitude;
    }

    public Double getCheckOutLatitude() {
        return checkOutLatitude;
    }

    public Double getCheckOutLongitude() {
        return checkOutLongitude;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public boolean isLate() {
        return late;
    }

    public void updateLate(boolean late) {
        this.late = late;
    }
}
