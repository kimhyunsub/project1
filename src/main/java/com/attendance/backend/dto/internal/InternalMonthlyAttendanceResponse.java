package com.attendance.backend.dto.internal;

import java.util.List;

public record InternalMonthlyAttendanceResponse(
    InternalMonthlyAttendanceSummaryResponse summary,
    List<InternalMonthlyAttendanceEmployeeRowResponse> employees,
    List<InternalMonthlyAttendanceRecordRowResponse> records,
    InternalMonthlyAttendanceEmployeeDetailResponse selectedEmployeeDetail
) {
}
