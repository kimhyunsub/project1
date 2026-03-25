package com.attendance.backend.service;

import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.EmployeeRole;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.dto.auth.ChangePasswordRequest;
import com.attendance.backend.dto.auth.ChangePasswordResponse;
import com.attendance.backend.dto.auth.LoginRequest;
import com.attendance.backend.dto.auth.LoginResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.exception.UnauthorizedException;
import com.attendance.backend.security.JwtTokenProvider;
import java.time.format.DateTimeFormatter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final EmployeeRepository employeeRepository;
    private final CompanySettingRepository companySettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
        EmployeeRepository employeeRepository,
        CompanySettingRepository companySettingRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.employeeRepository = employeeRepository;
        this.companySettingRepository = companySettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmployeeCode(request.getEmployeeCode())
            .orElseThrow(() -> new UnauthorizedException("사번 또는 비밀번호가 올바르지 않습니다."));

        if (employee.isDeleted()) {
            throw new UnauthorizedException("삭제된 계정입니다. 관리자에게 문의해 주세요.");
        }

        if (!employee.isActive()) {
            throw new UnauthorizedException("사용이 중지된 계정입니다. 관리자에게 문의해 주세요.");
        }

        boolean allowsPasswordlessFirstLogin =
            employee.getRole() == EmployeeRole.EMPLOYEE && employee.isPasswordChangeRequired();
        String requestPassword = request.getPassword();
        boolean passwordProvided = StringUtils.hasText(requestPassword);

        if (!allowsPasswordlessFirstLogin && !passwordProvided) {
            throw new UnauthorizedException("사번 또는 비밀번호가 올바르지 않습니다.");
        }

        if (passwordProvided && !passwordEncoder.matches(requestPassword, employee.getPassword())) {
            throw new UnauthorizedException("사번 또는 비밀번호가 올바르지 않습니다.");
        }

        CompanySetting companySetting = companySettingRepository.findByCompany(employee.getCompany())
            .orElse(null);
        String normalizedDeviceId = request.getDeviceId().trim();
        String normalizedDeviceName = request.getDeviceName() == null ? null : request.getDeviceName().trim();

        if (companySetting != null
            && companySetting.isEnforceSingleDeviceLogin()
            && employee.hasRegisteredDevice()
            && !employee.isRegisteredDevice(normalizedDeviceId)) {
            throw new UnauthorizedException("이미 다른 단말이 등록되어 있습니다. 관리자에게 단말 초기화를 요청해 주세요.");
        }

        employee.registerDevice(normalizedDeviceId, normalizedDeviceName);

        String token = jwtTokenProvider.generateToken(employee.getId(), employee.getEmployeeCode(), normalizedDeviceId);
        String accessTokenExpiresAt = ISO_DATE_TIME_FORMATTER.format(jwtTokenProvider.getExpiration(token));

        return new LoginResponse(
            token,
            "Bearer",
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getCompany().getName(),
            employee.getWorkplace() == null ? null : employee.getWorkplace().getName(),
            employee.getRole().name(),
            employee.getRole() == EmployeeRole.EMPLOYEE && employee.isPasswordChangeRequired(),
            accessTokenExpiresAt
        );
    }

    @Transactional
    public ChangePasswordResponse changePassword(Long employeeId, ChangePasswordRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));

        boolean canSkipCurrentPassword =
            employee.getRole() == EmployeeRole.EMPLOYEE && employee.isPasswordChangeRequired();
        String currentPassword = request.getCurrentPassword();

        if (!canSkipCurrentPassword && !StringUtils.hasText(currentPassword)) {
            throw new UnauthorizedException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (!canSkipCurrentPassword && !passwordEncoder.matches(currentPassword, employee.getPassword())) {
            throw new UnauthorizedException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), employee.getPassword())) {
            throw new BusinessException("새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.");
        }

        employee.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        employee.markPasswordChanged();

        return new ChangePasswordResponse("비밀번호가 변경되었습니다.");
    }
}
