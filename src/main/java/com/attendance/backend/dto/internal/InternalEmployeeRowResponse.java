package com.attendance.backend.dto.internal;

public record InternalEmployeeRowResponse(
    Long id,
    String employeeCode,
    String name,
    String workplaceName,
    String role,
    String workStartTime,
    String workEndTime,
    String attendanceState,
    String checkInTime,
    String checkOutTime,
    boolean deviceRegistered,
    boolean active,
    boolean deleted
) {
}
