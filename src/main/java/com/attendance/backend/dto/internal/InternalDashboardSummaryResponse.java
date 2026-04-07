package com.attendance.backend.dto.internal;

public record InternalDashboardSummaryResponse(
    int totalEmployees,
    int presentCount,
    int lateCount,
    int absentCount,
    int checkedOutCount
) {
}
