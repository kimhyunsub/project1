package com.attendance.backend.dto.internal;

import java.util.List;

public record InternalMonthlyAttendanceEmployeeDetailResponse(
    String employeeCode,
    String employeeName,
    String workplaceName,
    String role,
    int attendanceDays,
    int lateDays,
    int checkedOutDays,
    List<InternalMonthlyAttendanceRecordRowResponse> records
) {
}
