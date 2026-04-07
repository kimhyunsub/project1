package com.attendance.backend.dto.internal;

public record InternalEmployeeInviteResponse(
    String employeeCode,
    String employeeName,
    String role,
    String workplaceName,
    String inviteUrl,
    String expiresAt,
    String message
) {
}
