package com.attendance.backend.controller;

import com.attendance.backend.dto.admin.CompanySettingResponse;
import com.attendance.backend.dto.admin.CreateEmployeeInviteRequest;
import com.attendance.backend.dto.admin.CreateEmployeeInviteResponse;
import com.attendance.backend.dto.admin.CreateWorkplaceRequest;
import com.attendance.backend.dto.admin.EmployeeSummaryResponse;
import com.attendance.backend.dto.admin.TodayAttendanceOverviewResponse;
import com.attendance.backend.dto.admin.UpdateAttendanceRadiusRequest;
import com.attendance.backend.dto.admin.UpdateCompanyLocationRequest;
import com.attendance.backend.dto.admin.WorkplaceResponse;
import com.attendance.backend.security.CustomUserDetails;
import com.attendance.backend.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeSummaryResponse>> getEmployees(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(adminService.getEmployees(userDetails.getEmployeeId()));
    }

    @PostMapping("/employees/invite")
    public ResponseEntity<CreateEmployeeInviteResponse> createEmployeeInvite(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CreateEmployeeInviteRequest request
    ) {
        return ResponseEntity.ok(adminService.createEmployeeInvite(userDetails.getEmployeeId(), request));
    }

    @GetMapping("/attendance/today")
    public ResponseEntity<List<TodayAttendanceOverviewResponse>> getTodayAttendance(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(adminService.getTodayAttendance(userDetails.getEmployeeId()));
    }

    @GetMapping("/attendance/monthly/excel")
    public ResponseEntity<byte[]> downloadMonthlyAttendanceExcel(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam int year,
        @RequestParam int month
    ) {
        byte[] file = adminService.exportMonthlyAttendanceExcel(userDetails.getEmployeeId(), year, month);
        String fileName = "attendance-" + year + "-" + String.format("%02d", month) + ".xlsx";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .body(file);
    }

    @PatchMapping("/company/location")
    public ResponseEntity<CompanySettingResponse> updateCompanyLocation(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateCompanyLocationRequest request
    ) {
        return ResponseEntity.ok(adminService.updateCompanyLocation(userDetails.getEmployeeId(), request));
    }

    @PatchMapping("/company/radius")
    public ResponseEntity<CompanySettingResponse> updateAttendanceRadius(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateAttendanceRadiusRequest request
    ) {
        return ResponseEntity.ok(adminService.updateAttendanceRadius(userDetails.getEmployeeId(), request));
    }

    @PostMapping("/workplaces")
    public ResponseEntity<WorkplaceResponse> createWorkplace(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CreateWorkplaceRequest request
    ) {
        return ResponseEntity.ok(adminService.createWorkplace(userDetails.getEmployeeId(), request));
    }
}
