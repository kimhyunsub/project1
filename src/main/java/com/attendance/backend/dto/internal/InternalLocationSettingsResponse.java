package com.attendance.backend.dto.internal;

import java.time.LocalTime;
import java.util.List;

public record InternalLocationSettingsResponse(
    String companyName,
    Double latitude,
    Double longitude,
    Integer allowedRadiusMeters,
    LocalTime lateAfterTime,
    String noticeMessage,
    String mobileSkinKey,
    boolean enforceSingleDeviceLogin,
    boolean workplaceScopedAdmin,
    Long assignedWorkplaceId,
    List<InternalWorkplaceLocationResponse> workplaces
) {
}
