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
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_action_logs")
public class AttendanceActionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceActionType actionType;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double accuracyMeters;

    @Column(nullable = false)
    private Instant capturedAt;

    private Double distanceMeters;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false, length = 500)
    private String message;

    protected AttendanceActionLog() {
    }

    public AttendanceActionLog(
        Employee employee,
        AttendanceActionType actionType,
        LocalDate attendanceDate,
        Double latitude,
        Double longitude,
        Double accuracyMeters,
        Instant capturedAt,
        Double distanceMeters,
        boolean success,
        String message
    ) {
        this.employee = employee;
        this.actionType = actionType;
        this.attendanceDate = attendanceDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.capturedAt = capturedAt;
        this.distanceMeters = distanceMeters;
        this.success = success;
        this.message = message;
    }
}
