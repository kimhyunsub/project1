package com.attendance.backend.controller;

import com.attendance.backend.config.InternalApiProperties;
import com.attendance.backend.dto.internal.InternalEmployeeFormResponse;
import com.attendance.backend.dto.internal.InternalEmployeeInviteCreateRequest;
import com.attendance.backend.dto.internal.InternalEmployeeInviteResponse;
import com.attendance.backend.dto.internal.InternalEmployeePageResponse;
import com.attendance.backend.dto.internal.InternalEmployeeUploadResponse;
import com.attendance.backend.dto.internal.InternalEmployeeUpsertRequest;
import com.attendance.backend.dto.internal.InternalMessageResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/internal/admin/employees")
public class InternalAdminEmployeeController {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String ADMIN_CODE_HEADER = "X-Admin-Employee-Code";

    private final AdminService adminService;
    private final InternalApiProperties internalApiProperties;

    public InternalAdminEmployeeController(AdminService adminService, InternalApiProperties internalApiProperties) {
        this.adminService = adminService;
        this.internalApiProperties = internalApiProperties;
    }

    @GetMapping
    public ResponseEntity<InternalEmployeePageResponse> getEmployees(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestParam(name = "showDeleted", defaultValue = "false") boolean showDeleted,
        @RequestParam(name = "workplaceId", required = false) Long workplaceId,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "pageSize", defaultValue = "12") int pageSize
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(
            adminService.getEmployeePageForAdmin(adminEmployeeCode, showDeleted, workplaceId, page, pageSize)
        );
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<InternalEmployeeFormResponse> getEmployee(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("employeeId") Long employeeId
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(adminService.getEmployeeFormForAdmin(adminEmployeeCode, employeeId));
    }

    @PostMapping
    public ResponseEntity<InternalMessageResponse> createEmployee(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestBody InternalEmployeeUpsertRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.createEmployeeForAdmin(adminEmployeeCode, request);
        return ResponseEntity.ok(new InternalMessageResponse("직원이 등록되었습니다."));
    }

    @PatchMapping("/{employeeId}")
    public ResponseEntity<InternalMessageResponse> updateEmployee(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("employeeId") Long employeeId,
        @RequestBody InternalEmployeeUpsertRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.updateEmployeeForAdmin(adminEmployeeCode, employeeId, request);
        return ResponseEntity.ok(new InternalMessageResponse("직원 정보가 수정되었습니다."));
    }

    @PostMapping("/{employeeId}/invite-link")
    public ResponseEntity<InternalEmployeeInviteResponse> createInviteLink(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("employeeId") Long employeeId
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(adminService.createEmployeeInviteForAdmin(adminEmployeeCode, employeeId));
    }

    @PostMapping("/invite")
    public ResponseEntity<InternalEmployeeInviteResponse> createInvite(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestBody InternalEmployeeInviteCreateRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(adminService.createEmployeeInviteForAdmin(adminEmployeeCode, request));
    }

    @PostMapping("/{employeeId}/usage")
    public ResponseEntity<InternalMessageResponse> updateUsage(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("employeeId") Long employeeId,
        @RequestParam("active") boolean active
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.updateEmployeeUsageForAdmin(adminEmployeeCode, employeeId, active);
        return ResponseEntity.ok(new InternalMessageResponse(active ? "직원이 다시 사용 상태로 변경되었습니다." : "직원이 사용 중지되었습니다."));
    }

    @PostMapping("/{employeeId}/device-reset")
    public ResponseEntity<InternalMessageResponse> resetDevice(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("employeeId") Long employeeId
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.resetEmployeeDeviceForAdmin(adminEmployeeCode, employeeId);
        return ResponseEntity.ok(new InternalMessageResponse("등록된 단말이 초기화되었습니다."));
    }

    @PostMapping("/{employeeId}/delete")
    public ResponseEntity<InternalMessageResponse> deleteEmployee(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("employeeId") Long employeeId
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.deleteEmployeeForAdmin(adminEmployeeCode, employeeId);
        return ResponseEntity.ok(new InternalMessageResponse("직원이 삭제 목록으로 이동되었습니다."));
    }

    @PostMapping("/{employeeId}/restore")
    public ResponseEntity<InternalMessageResponse> restoreEmployee(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @PathVariable("employeeId") Long employeeId
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        adminService.restoreEmployeeForAdmin(adminEmployeeCode, employeeId);
        return ResponseEntity.ok(new InternalMessageResponse("직원이 복구되었습니다."));
    }

    @PostMapping("/upload")
    public ResponseEntity<InternalEmployeeUploadResponse> uploadEmployees(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestPart("employeeFile") MultipartFile employeeFile
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(adminService.uploadEmployeesForAdmin(adminEmployeeCode, employeeFile));
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
