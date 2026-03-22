package com.attendance.backend.controller;

import com.attendance.backend.dto.attendance.CheckInRequest;
import com.attendance.backend.dto.attendance.CheckInResponse;
import com.attendance.backend.dto.attendance.CheckOutRequest;
import com.attendance.backend.dto.attendance.CheckOutResponse;
import com.attendance.backend.dto.attendance.TodayAttendanceStatusResponse;
import com.attendance.backend.dto.admin.CompanySettingResponse;
import com.attendance.backend.security.CustomUserDetails;
import com.attendance.backend.service.AttendanceService;
import jakarta.validation.Valid;
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

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponse> checkIn(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CheckInRequest request
    ) {
        return ResponseEntity.ok(attendanceService.checkIn(userDetails.getEmployeeId(), request));
    }

    @PostMapping("/check-out")
    public ResponseEntity<CheckOutResponse> checkOut(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CheckOutRequest request
    ) {
        return ResponseEntity.ok(attendanceService.checkOut(userDetails.getEmployeeId(), request));
    }

    @GetMapping("/today")
    public ResponseEntity<TodayAttendanceStatusResponse> getTodayStatus(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(attendanceService.getTodayStatus(userDetails.getEmployeeId()));
    }

    @GetMapping("/company-setting")
    public ResponseEntity<CompanySettingResponse> getCompanySetting(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(attendanceService.getCompanySetting(userDetails.getEmployeeId()));
    }

    @GetMapping("/public/company-setting")
    public ResponseEntity<CompanySettingResponse> getPublicCompanySetting() {
        return ResponseEntity.ok(attendanceService.getPublicCompanySetting());
    }
}
