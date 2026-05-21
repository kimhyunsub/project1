package com.attendance.backend.dto.internal;

public class InternalWorkplaceUpsertRequest {

    private String name;
    private Double latitude;
    private Double longitude;
    private Integer allowedRadiusMeters;
    private String noticeMessage;
    private boolean workRequestApprovalRequired = true;
    private boolean workRequestEnabled = true;
    private boolean enforceSingleDeviceLogin = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getAllowedRadiusMeters() {
        return allowedRadiusMeters;
    }

    public void setAllowedRadiusMeters(Integer allowedRadiusMeters) {
        this.allowedRadiusMeters = allowedRadiusMeters;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public void setNoticeMessage(String noticeMessage) {
        this.noticeMessage = noticeMessage;
    }

    public boolean isWorkRequestApprovalRequired() {
        return workRequestApprovalRequired;
    }

    public void setWorkRequestApprovalRequired(boolean workRequestApprovalRequired) {
        this.workRequestApprovalRequired = workRequestApprovalRequired;
    }

    public boolean isWorkRequestEnabled() {
        return workRequestEnabled;
    }

    public void setWorkRequestEnabled(boolean workRequestEnabled) {
        this.workRequestEnabled = workRequestEnabled;
    }

    public boolean isEnforceSingleDeviceLogin() {
        return enforceSingleDeviceLogin;
    }

    public void setEnforceSingleDeviceLogin(boolean enforceSingleDeviceLogin) {
        this.enforceSingleDeviceLogin = enforceSingleDeviceLogin;
    }
}
