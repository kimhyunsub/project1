package com.attendance.backend.controller;

import com.attendance.backend.config.InternalApiProperties;
import com.attendance.backend.dto.internal.InternalCompanyLocationUpdateRequest;
import com.attendance.backend.dto.internal.InternalLocationSettingsResponse;
import com.attendance.backend.dto.internal.InternalMessageResponse;
import com.attendance.backend.dto.internal.InternalWorkplaceUpsertRequest;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/admin/settings/location")
public class InternalAdminLocationController {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String ADMIN_CODE_HEADER = "X-Admin-Employee-Code";

    private final AdminService adminService;
    private final InternalApiProperties internalApiProperties;

    public InternalAdminLocationController(AdminService adminService, InternalApiProperties internalApiProperties) {
        this.adminService = adminService;
        this.internalApiProperties = internalApiProperties;
    }

    @GetMapping
    public ResponseEntity<InternalLocationSettingsResponse> getLocationSettings(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(adminService.getLocationSettingsForAdmin(adminEmployeeCode));
    }

    @PatchMapping
    public ResponseEntity<InternalMessageResponse> updateCompanySettings(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestBody InternalCompanyLocationUpdateRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.updateCompanySettingsForAdmin(adminEmployeeCode, request);
        return ResponseEntity.ok(new InternalMessageResponse("회사 위치가 저장되었습니다."));
    }

    @PostMapping("/workplaces")
    public ResponseEntity<InternalMessageResponse> createWorkplace(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestBody InternalWorkplaceUpsertRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.createWorkplaceForAdmin(adminEmployeeCode, request);
        return ResponseEntity.ok(new InternalMessageResponse("사업장이 추가되었습니다."));
    }

    @PatchMapping("/workplaces/{workplaceId}")
    public ResponseEntity<InternalMessageResponse> updateWorkplace(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("workplaceId") Long workplaceId,
        @RequestBody InternalWorkplaceUpsertRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.updateWorkplaceForAdmin(adminEmployeeCode, workplaceId, request);
        return ResponseEntity.ok(new InternalMessageResponse("사업장 위치가 저장되었습니다."));
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
