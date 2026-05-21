package com.attendance.backend.dto.internal;

public class InternalCompanyLocationUpdateRequest {

    private String companyName;
    private Double latitude;
    private Double longitude;
    private Integer allowedRadiusMeters;
    private String noticeMessage;
    private String mobileSkinKey;
    private boolean enforceSingleDeviceLogin;
    private boolean workRequestApprovalRequired;
    private boolean workRequestEnabled = true;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public String getMobileSkinKey() {
        return mobileSkinKey;
    }

    public void setMobileSkinKey(String mobileSkinKey) {
        this.mobileSkinKey = mobileSkinKey;
    }

    public boolean isEnforceSingleDeviceLogin() {
        return enforceSingleDeviceLogin;
    }

    public void setEnforceSingleDeviceLogin(boolean enforceSingleDeviceLogin) {
        this.enforceSingleDeviceLogin = enforceSingleDeviceLogin;
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
}
