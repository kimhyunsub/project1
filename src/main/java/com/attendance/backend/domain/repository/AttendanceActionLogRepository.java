package com.attendance.backend.domain.repository;

import com.attendance.backend.domain.entity.AttendanceActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceActionLogRepository extends JpaRepository<AttendanceActionLog, Long> {
}
