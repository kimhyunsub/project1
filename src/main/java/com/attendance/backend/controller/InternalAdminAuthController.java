package com.attendance.backend.controller;

import com.attendance.backend.config.InternalApiProperties;
import com.attendance.backend.dto.internal.InternalAdminUserDetailsResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/admin/auth")
public class InternalAdminAuthController {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final AdminService adminService;
    private final InternalApiProperties internalApiProperties;

    public InternalAdminAuthController(AdminService adminService, InternalApiProperties internalApiProperties) {
        this.adminService = adminService;
        this.internalApiProperties = internalApiProperties;
    }

    @GetMapping("/users/{employeeCode}")
    public ResponseEntity<InternalAdminUserDetailsResponse> getAdminUserDetails(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @PathVariable("employeeCode") String employeeCode
    ) {
        validateApiKey(apiKey);
        return ResponseEntity.ok(adminService.getAdminUserDetails(employeeCode));
    }

    private void validateApiKey(String apiKey) {
        if (!StringUtils.hasText(internalApiProperties.getKey()) || !internalApiProperties.getKey().equals(apiKey)) {
            throw new BusinessException("내부 API 인증에 실패했습니다.");
        }
    }
}
