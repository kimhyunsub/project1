package com.attendance.backend.dto.internal;

import java.util.List;

public record InternalSqlQueryResultResponse(
    List<String> columns,
    List<List<String>> rows,
    int rowLimit,
    boolean truncated
) {
}
