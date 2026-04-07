package com.attendance.backend.dto.internal;

public record InternalAdminUserDetailsResponse(
    String employeeCode,
    String password,
    String role,
    boolean active
) {
}
