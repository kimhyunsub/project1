package com.attendance.backend.domain.repository;

import com.attendance.backend.domain.entity.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @EntityGraph(attributePaths = {"employee", "employee.company"})
    List<AttendanceRecord> findAllByLateTrue();

    @EntityGraph(attributePaths = {"employee", "employee.company"})
    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    @EntityGraph(attributePaths = {"employee", "employee.company"})
    List<AttendanceRecord> findAllByEmployeeCompanyIdAndAttendanceDate(Long companyId, LocalDate attendanceDate);

    @EntityGraph(attributePaths = {"employee", "employee.company"})
    List<AttendanceRecord> findAllByEmployeeCompanyIdAndAttendanceDateBetween(
        Long companyId,
        LocalDate startDate,
        LocalDate endDate
    );
}
