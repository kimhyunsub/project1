package com.attendance.backend.dto.admin;

public class CreateEmployeeInviteResponse {

    private final Long employeeId;
    private final String employeeCode;
    private final String employeeName;
    private final String role;
    private final Long companyId;
    private final String companyName;
    private final Long workplaceId;
    private final String workplaceName;
    private final String inviteToken;
    private final String inviteUrl;
    private final String expiresAt;
    private final String message;

    public CreateEmployeeInviteResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String role,
        Long companyId,
        String companyName,
        Long workplaceId,
        String workplaceName,
        String inviteToken,
        String inviteUrl,
        String expiresAt,
        String message
    ) {
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.role = role;
        this.companyId = companyId;
        this.companyName = companyName;
        this.workplaceId = workplaceId;
        this.workplaceName = workplaceName;
        this.inviteToken = inviteToken;
        this.inviteUrl = inviteUrl;
        this.expiresAt = expiresAt;
        this.message = message;
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

    public String getRole() {
        return role;
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

    public String getInviteToken() {
        return inviteToken;
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getMessage() {
        return message;
    }
}
