package com.attendance.backend.controller;

import com.attendance.backend.dto.attendance.CheckInRequest;
import com.attendance.backend.dto.attendance.CheckInResponse;
import com.attendance.backend.dto.attendance.CheckOutRequest;
import com.attendance.backend.dto.attendance.CheckOutResponse;
import com.attendance.backend.dto.attendance.CreateWorkRequestRequest;
import com.attendance.backend.dto.attendance.TodayAttendanceStatusResponse;
import com.attendance.backend.dto.attendance.WorkRequestActionResponse;
import com.attendance.backend.dto.attendance.WorkRequestCreateResponse;
import com.attendance.backend.dto.attendance.WorkRequestListResponse;
import com.attendance.backend.dto.admin.CompanySettingResponse;
import com.attendance.backend.exception.UnauthorizedException;
import com.attendance.backend.security.CustomUserDetails;
import com.attendance.backend.service.AttendanceService;
import com.attendance.backend.service.WorkRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;
    private final WorkRequestService workRequestService;

    public AttendanceController(AttendanceService attendanceService, WorkRequestService workRequestService) {
        this.attendanceService = attendanceService;
        this.workRequestService = workRequestService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponse> checkIn(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CheckInRequest request
    ) {
        return ResponseEntity.ok(attendanceService.checkIn(requireEmployeeId(userDetails), request));
    }

    @PostMapping("/check-out")
    public ResponseEntity<CheckOutResponse> checkOut(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CheckOutRequest request
    ) {
        return ResponseEntity.ok(attendanceService.checkOut(requireEmployeeId(userDetails), request));
    }

    @GetMapping("/today")
    public ResponseEntity<TodayAttendanceStatusResponse> getTodayStatus(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(attendanceService.getTodayStatus(requireEmployeeId(userDetails)));
    }

    @GetMapping("/company-setting")
    public ResponseEntity<CompanySettingResponse> getCompanySetting(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(attendanceService.getCompanySetting(requireEmployeeId(userDetails)));
    }

    @GetMapping("/public/company-setting")
    public ResponseEntity<CompanySettingResponse> getPublicCompanySetting() {
        return ResponseEntity.ok(attendanceService.getPublicCompanySetting());
    }

    @PostMapping("/work-requests")
    public ResponseEntity<WorkRequestCreateResponse> createWorkRequest(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CreateWorkRequestRequest request
    ) {
        return ResponseEntity.ok(workRequestService.createRequest(requireEmployeeId(userDetails), request));
    }

    @GetMapping("/work-requests")
    public ResponseEntity<WorkRequestListResponse> getWorkRequests(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long employeeId = requireEmployeeId(userDetails);
        try {
            return ResponseEntity.ok(workRequestService.getRequests(employeeId));
        } catch (RuntimeException exception) {
            log.warn("Failed to get work requests. employeeId={}", employeeId, exception);
            return ResponseEntity.ok(new WorkRequestListResponse(true, true, List.of()));
        }
    }

    @PostMapping("/work-requests/{requestId}/cancel")
    public ResponseEntity<WorkRequestActionResponse> cancelWorkRequest(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @org.springframework.web.bind.annotation.PathVariable Long requestId
    ) {
        return ResponseEntity.ok(workRequestService.cancelRequest(requireEmployeeId(userDetails), requestId));
    }

    private Long requireEmployeeId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getEmployeeId() == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return userDetails.getEmployeeId();
    }
}
