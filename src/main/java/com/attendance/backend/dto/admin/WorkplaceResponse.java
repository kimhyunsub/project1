package com.attendance.backend.dto.admin;

public class WorkplaceResponse {

    private final Long companyId;
    private final String companyName;
    private final Long workplaceId;
    private final String workplaceName;
    private final Double latitude;
    private final Double longitude;
    private final Integer allowedRadiusMeters;
    private final String noticeMessage;
    private final String message;

    public WorkplaceResponse(
        Long companyId,
        String companyName,
        Long workplaceId,
        String workplaceName,
        Double latitude,
        Double longitude,
        Integer allowedRadiusMeters,
        String noticeMessage,
        String message
    ) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.workplaceId = workplaceId;
        this.workplaceName = workplaceName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
        this.noticeMessage = noticeMessage;
        this.message = message;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Long getWorkplaceId() {
        return workplaceId;
    }

    public String getWorkplaceName() {
        return workplaceName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getAllowedRadiusMeters() {
        return allowedRadiusMeters;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public String getMessage() {
        return message;
    }
}
