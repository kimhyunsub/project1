package com.attendance.backend.dto.internal;

import java.util.List;

public record InternalDashboardResponse(
    InternalDashboardSummaryResponse summary,
    List<InternalAttendanceRowResponse> attendances
) {
}
