package com.attendance.backend.dto.internal;

public record InternalEmployeeFormResponse(
    Long id,
    String employeeCode,
    String name,
    String role,
    String workStartTime,
    String workEndTime,
    Long workplaceId
) {
}
