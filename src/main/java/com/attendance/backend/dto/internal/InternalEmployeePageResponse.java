package com.attendance.backend.dto.internal;

import java.util.List;

public record InternalEmployeePageResponse(
    List<InternalEmployeeRowResponse> employees,
    int currentPage,
    int totalPages,
    long totalCount,
    int pageSize,
    boolean hasPrevious,
    boolean hasNext
) {
}
