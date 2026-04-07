package com.attendance.backend.controller;

import com.attendance.backend.config.InternalApiProperties;
import com.attendance.backend.dto.internal.InternalMessageResponse;
import com.attendance.backend.dto.internal.InternalSqlQueryRequest;
import com.attendance.backend.dto.internal.InternalSqlQueryResultResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.service.AdminService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/admin/sql")
public class InternalAdminSqlController {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String ADMIN_CODE_HEADER = "X-Admin-Employee-Code";

    private final AdminService adminService;
    private final InternalApiProperties internalApiProperties;

    public InternalAdminSqlController(AdminService adminService, InternalApiProperties internalApiProperties) {
        this.adminService = adminService;
        this.internalApiProperties = internalApiProperties;
    }

    @PostMapping("/query")
    public ResponseEntity<InternalSqlQueryResultResponse> executeQuery(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestBody InternalSqlQueryRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        return ResponseEntity.ok(adminService.executeReadOnlySqlForAdmin(adminEmployeeCode, request.getQueryText()));
    }

    @PostMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestHeader(ADMIN_CODE_HEADER) String adminEmployeeCode,
        @RequestBody InternalSqlQueryRequest request
    ) {
        validateHeaders(apiKey, adminEmployeeCode);
        byte[] fileBytes = adminService.exportSqlQueryExcelForAdmin(adminEmployeeCode, request.getQueryText());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sql-report.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .body(fileBytes);
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
