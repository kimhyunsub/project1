package com.attendance.backend.dto.attendance;

import java.util.List;

public record WorkRequestListResponse(
    boolean approvalRequired,
    boolean enabled,
    List<WorkRequestResponse> requests
) {
}
