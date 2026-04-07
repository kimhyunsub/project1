package com.attendance.backend.dto.internal;

public record InternalAttendanceRowResponse(
    String employeeCode,
    String employeeName,
    String workplaceName,
    String role,
    String state,
    String checkInTime,
    String checkOutTime,
    String note
) {
}
