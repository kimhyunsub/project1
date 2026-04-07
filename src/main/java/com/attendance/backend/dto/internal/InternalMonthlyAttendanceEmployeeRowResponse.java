package com.attendance.backend.dto.internal;

public record InternalMonthlyAttendanceEmployeeRowResponse(
    String employeeCode,
    String employeeName,
    String workplaceName,
    String role,
    int attendanceDays,
    int lateDays,
    int checkedOutDays,
    String lastAttendanceDate,
    String lastState
) {
}
