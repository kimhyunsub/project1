package com.attendance.backend.controller;

import com.attendance.backend.config.InternalApiProperties;
import com.attendance.backend.dto.internal.InternalMonthlyAttendanceResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/admin/attendance/monthly")
public class InternalAdminMonthlyAttendanceController {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String ADMIN_CODE_HEADER = "X-Admin-Employee-Code";

    private final AdminService adminService;
    private final InternalApiProperties internalApiProperties;

    public InternalAdminMonthlyAttendanceController(AdminService adminService, InternalApiProperties internalApiProperties) {
        this.adminService = adminService;
        this.internalApiProperties = internalApiProperties;
    }

    @GetMapping
    public ResponseEntity<InternalMonthlyAttendanceResponse> getMonthlyAttendance(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestParam(name = "year") int year,
        @RequestParam(name = "month") int month,
        @RequestParam(name = "employeeCode", required = false) String selectedEmployeeCode,
        @RequestParam(name = "workplaceId", required = false) Long workplaceId
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(
            adminService.getMonthlyAttendanceForAdmin(adminEmployeeCode, year, month, selectedEmployeeCode, workplaceId)
        );
    }

    private void validateHeaders(String apiKey, String adminEmployeeCode) {
        if (!StringUtils.hasText(internalApiProperties.getKey()) || !internalApiProperties.getKey().equals(apiKey)) {
            throw new BusinessException("내부 API 인증에 실패했습니다.");
        }

        if (!StringUtils.hasText(adminEmployeeCode)) {
            throw new BusinessException("관리자 식별자가 필요합니다.");
        }
    }
}
