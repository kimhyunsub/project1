package com.attendance.backend.dto.auth;

public class CompanySignupResponse {

    private final Long companyId;
    private final String companyName;
    private final String adminEmployeeCode;
    private final String adminName;
    private final String message;

    public CompanySignupResponse(
        Long companyId,
        String companyName,
        String adminEmployeeCode,
        String adminName,
        String message
    ) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.adminEmployeeCode = adminEmployeeCode;
        this.adminName = adminName;
        this.message = message;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getAdminEmployeeCode() {
        return adminEmployeeCode;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getMessage() {
        return message;
    }
}
