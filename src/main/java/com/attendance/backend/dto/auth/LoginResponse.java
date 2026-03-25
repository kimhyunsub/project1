package com.attendance.backend.dto.auth;

public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final Long employeeId;
    private final String employeeCode;
    private final String employeeName;
    private final String companyName;
    private final String workplaceName;
    private final String role;
    private final boolean passwordChangeRequired;
    private final String accessTokenExpiresAt;

    public LoginResponse(
        String accessToken,
        String tokenType,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String companyName,
        String workplaceName,
        String role,
        boolean passwordChangeRequired,
        String accessTokenExpiresAt
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.companyName = companyName;
        this.workplaceName = workplaceName;
        this.role = role;
        this.passwordChangeRequired = passwordChangeRequired;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getWorkplaceName() {
        return workplaceName;
    }

    public String getRole() {
        return role;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public String getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }
}
