package com.attendance.backend.controller;

import com.attendance.backend.dto.auth.ChangePasswordRequest;
import com.attendance.backend.dto.auth.ChangePasswordResponse;
import com.attendance.backend.dto.auth.CompanySignupRequest;
import com.attendance.backend.dto.auth.CompanySignupResponse;
import com.attendance.backend.dto.auth.InviteActivateRequest;
import com.attendance.backend.dto.auth.InvitePreviewResponse;
import com.attendance.backend.dto.auth.LoginRequest;
import com.attendance.backend.dto.auth.LoginResponse;
import com.attendance.backend.security.CustomUserDetails;
import com.attendance.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/company-signup")
    public ResponseEntity<CompanySignupResponse> companySignup(@Valid @RequestBody CompanySignupRequest request) {
        return ResponseEntity.ok(authService.companySignup(request));
    }

    @GetMapping("/invite/preview")
    public ResponseEntity<InvitePreviewResponse> previewInvite(@RequestParam String token) {
        return ResponseEntity.ok(authService.previewInvite(token));
    }

    @PostMapping("/invite/activate")
    public ResponseEntity<LoginResponse> activateInvite(@Valid @RequestBody InviteActivateRequest request) {
        return ResponseEntity.ok(authService.activateInvite(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(authService.changePassword(userDetails.getEmployeeId(), request));
    }
}
