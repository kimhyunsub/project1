package com.attendance.backend.service;

import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.dto.auth.LoginRequest;
import com.attendance.backend.dto.auth.LoginResponse;
import com.attendance.backend.exception.UnauthorizedException;
import com.attendance.backend.security.JwtTokenProvider;
import java.time.format.DateTimeFormatter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
        EmployeeRepository employeeRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmployeeCode(request.getEmployeeCode())
            .orElseThrow(() -> new UnauthorizedException("사번 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            throw new UnauthorizedException("사번 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!employee.isActive()) {
            throw new UnauthorizedException("사용이 중지된 계정입니다. 관리자에게 문의해 주세요.");
        }

        String normalizedDeviceId = request.getDeviceId().trim();
        String normalizedDeviceName = request.getDeviceName() == null ? null : request.getDeviceName().trim();

        if (employee.hasRegisteredDevice() && !employee.isRegisteredDevice(normalizedDeviceId)) {
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
            employee.getRole().name(),
            accessTokenExpiresAt
        );
    }
}
