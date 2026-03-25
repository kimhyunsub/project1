package com.attendance.backend.dto.attendance;

import com.attendance.backend.domain.entity.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TodayAttendanceStatusResponse {

    private final boolean checkedIn;
    private final LocalDate attendanceDate;
    private final LocalDateTime checkInTime;
    private final LocalDateTime checkOutTime;
    private final AttendanceStatus status;
    private final String companyName;
    private final String workplaceName;

    public TodayAttendanceStatusResponse(
        boolean checkedIn,
        LocalDate attendanceDate,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        AttendanceStatus status,
        String companyName,
        String workplaceName
    ) {
        this.checkedIn = checkedIn;
        this.attendanceDate = attendanceDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.companyName = companyName;
        this.workplaceName = workplaceName;
    }

    public boolean isCheckedIn() {
        return checkedIn;
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

    public AttendanceStatus getStatus() {
        return status;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getWorkplaceName() {
        return workplaceName;
    }
}
