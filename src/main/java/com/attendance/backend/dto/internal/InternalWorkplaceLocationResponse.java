package com.attendance.backend.dto.internal;

public record InternalWorkplaceLocationResponse(
    Long id,
    String name,
    Double latitude,
    Double longitude,
    Integer allowedRadiusMeters,
    String noticeMessage
) {
}
