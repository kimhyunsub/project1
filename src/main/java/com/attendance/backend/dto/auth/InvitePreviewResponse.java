package com.attendance.backend.dto.auth;

public class InvitePreviewResponse {

    private final String employeeName;
    private final String employeeCode;
    private final String companyName;
    private final Long companyId;
    private final String workplaceName;
    private final Long workplaceId;
    private final String role;
    private final String expiresAt;
    private final String message;

    public InvitePreviewResponse(
        String employeeName,
        String employeeCode,
        String companyName,
        Long companyId,
        String workplaceName,
        Long workplaceId,
        String role,
        String expiresAt,
        String message
    ) {
        this.employeeName = employeeName;
        this.employeeCode = employeeCode;
        this.companyName = companyName;
        this.companyId = companyId;
        this.workplaceName = workplaceName;
        this.workplaceId = workplaceId;
        this.role = role;
        this.expiresAt = expiresAt;
        this.message = message;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getWorkplaceName() {
        return workplaceName;
    }

    public Long getWorkplaceId() {
        return workplaceId;
    }

    public String getRole() {
        return role;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getMessage() {
        return message;
    }
}
