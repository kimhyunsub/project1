package com.attendance.backend.dto.internal;

import java.util.List;

public record InternalEmployeeUploadResponse(
        int successCount,
        int failureCount,
        List<String> failureMessages
) {
}
