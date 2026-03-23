package com.attendance.backend.service;

import com.attendance.backend.domain.entity.AttendanceActionLog;
import com.attendance.backend.domain.entity.AttendanceActionType;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.repository.AttendanceActionLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceActionLogService {

    private final AttendanceActionLogRepository attendanceActionLogRepository;

    public AttendanceActionLogService(AttendanceActionLogRepository attendanceActionLogRepository) {
        this.attendanceActionLogRepository = attendanceActionLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAttempt(
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
        attendanceActionLogRepository.save(
            new AttendanceActionLog(
                employee,
                actionType,
                attendanceDate,
                latitude,
                longitude,
                accuracyMeters,
                capturedAt,
                distanceMeters,
                success,
                truncate(message)
            )
        );
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "기록 없음";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
