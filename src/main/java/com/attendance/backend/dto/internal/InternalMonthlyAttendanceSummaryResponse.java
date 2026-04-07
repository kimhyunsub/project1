package com.attendance.backend.dto.internal;

public record InternalMonthlyAttendanceSummaryResponse(
    String monthLabel,
    int totalEmployees,
    int attendedEmployees,
    int attendanceCount,
    int lateCount,
    int checkedOutCount
) {
}
