package com.attendance.backend.dto.admin;

import java.time.LocalTime;

public class CompanySettingResponse {

    private final Long companyId;
    private final String companyName;
    private final Long workplaceId;
    private final String workplaceName;
    private final Double latitude;
    private final Double longitude;
    private final Integer allowedRadiusMeters;
    private final LocalTime lateAfterTime;
    private final String noticeMessage;
    private final String mobileSkinKey;
    private final boolean enforceSingleDeviceLogin;
    private final boolean workRequestApprovalRequired;
    private final boolean workRequestEnabled;
    private final String message;

    public CompanySettingResponse(
        Long companyId,
        String companyName,
        Long workplaceId,
        String workplaceName,
        Double latitude,
        Double longitude,
        Integer allowedRadiusMeters,
        LocalTime lateAfterTime,
        String noticeMessage,
        String mobileSkinKey,
        boolean enforceSingleDeviceLogin,
        boolean workRequestApprovalRequired,
        boolean workRequestEnabled,
        String message
    ) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.workplaceId = workplaceId;
        this.workplaceName = workplaceName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
        this.lateAfterTime = lateAfterTime;
        this.noticeMessage = noticeMessage;
        this.mobileSkinKey = mobileSkinKey;
        this.enforceSingleDeviceLogin = enforceSingleDeviceLogin;
        this.workRequestApprovalRequired = workRequestApprovalRequired;
        this.workRequestEnabled = workRequestEnabled;
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

    public LocalTime getLateAfterTime() {
        return lateAfterTime;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public String getMobileSkinKey() {
        return mobileSkinKey;
    }

    public boolean isEnforceSingleDeviceLogin() {
        return enforceSingleDeviceLogin;
    }

    public boolean isWorkRequestApprovalRequired() {
        return workRequestApprovalRequired;
    }

    public boolean isWorkRequestEnabled() {
        return workRequestEnabled;
    }

    public String getMessage() {
        return message;
    }
}
