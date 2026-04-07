package com.attendance.backend.service;

import com.attendance.backend.domain.entity.Company;
import com.attendance.backend.domain.entity.CompanyPlan;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.EmployeeInvite;
import com.attendance.backend.domain.entity.EmployeeRole;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.repository.CompanyRepository;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import com.attendance.backend.domain.repository.EmployeeInviteRepository;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.dto.auth.ChangePasswordRequest;
import com.attendance.backend.dto.auth.ChangePasswordResponse;
import com.attendance.backend.dto.auth.CompanySignupRequest;
import com.attendance.backend.dto.auth.CompanySignupResponse;
import com.attendance.backend.dto.auth.InviteActivateRequest;
import com.attendance.backend.dto.auth.InvitePreviewResponse;
import com.attendance.backend.dto.auth.LoginRequest;
import com.attendance.backend.dto.auth.LoginResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.exception.UnauthorizedException;
import com.attendance.backend.security.JwtTokenProvider;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final LocalTime DEFAULT_LATE_AFTER_TIME = LocalTime.of(9, 0);
    private static final int DEFAULT_FREE_EMPLOYEE_LIMIT = 7;
    private static final int DEFAULT_FREE_WORKPLACE_LIMIT = 0;
    private static final int MAX_INVITE_USES = 2;

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingRepository companySettingRepository;
    private final EmployeeInviteRepository employeeInviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
        EmployeeRepository employeeRepository,
        CompanyRepository companyRepository,
        CompanySettingRepository companySettingRepository,
        EmployeeInviteRepository employeeInviteRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.companySettingRepository = companySettingRepository;
        this.employeeInviteRepository = employeeInviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public CompanySignupResponse companySignup(CompanySignupRequest request) {
        String companyName = request.getCompanyName().trim();
        String adminName = request.getAdminName().trim();
        String adminEmployeeCode = request.getAdminEmployeeCode().trim();

        if (companyRepository.existsByName(companyName)) {
            throw new BusinessException("이미 등록된 회사명입니다.");
        }

        if (employeeRepository.existsByEmployeeCode(adminEmployeeCode)) {
            throw new BusinessException("이미 사용 중인 관리자 아이디입니다.");
        }

        Company company = companyRepository.save(
            new Company(
                companyName,
                CompanyPlan.FREE,
                DEFAULT_FREE_EMPLOYEE_LIMIT,
                DEFAULT_FREE_WORKPLACE_LIMIT,
                request.getLatitude(),
                request.getLongitude()
            )
        );

        companySettingRepository.save(
            new CompanySetting(company, request.getAllowedRadiusMeters(), DEFAULT_LATE_AFTER_TIME)
        );

        Employee admin = new Employee(
            adminEmployeeCode,
            adminName,
            passwordEncoder.encode(request.getAdminPassword()),
            EmployeeRole.ADMIN,
            company
        );
        admin.markPasswordChanged();
        employeeRepository.save(admin);

        return new CompanySignupResponse(
            company.getId(),
            company.getName(),
            admin.getEmployeeCode(),
            admin.getName(),
            "회사 가입이 완료되었습니다. 관리자 계정으로 로그인한 뒤 사업장을 추가해 주세요."
        );
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

        String token = jwtTokenProvider.generateToken(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getCompany().getId(),
            normalizedDeviceId
        );
        String accessTokenExpiresAt = ISO_DATE_TIME_FORMATTER.format(jwtTokenProvider.getExpiration(token));

        return new LoginResponse(
            token,
            "Bearer",
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getCompany().getId(),
            employee.getCompany().getName(),
            employee.getWorkplace() == null ? null : employee.getWorkplace().getName(),
            employee.getRole().name(),
            employee.getRole() == EmployeeRole.EMPLOYEE && employee.isPasswordChangeRequired(),
            accessTokenExpiresAt
        );
    }

    public InvitePreviewResponse previewInvite(String token) {
        EmployeeInvite invite = getValidInvite(token);
        Employee employee = invite.getEmployee();

        return new InvitePreviewResponse(
            employee.getName(),
            employee.getEmployeeCode(),
            employee.getCompany().getName(),
            employee.getCompany().getId(),
            employee.getWorkplace() == null ? null : employee.getWorkplace().getName(),
            employee.getWorkplace() == null ? null : employee.getWorkplace().getId(),
            employee.getRole().name(),
            invite.getExpiresAt().toString(),
            "초대가 유효합니다. 이 링크는 최대 2회까지 사용할 수 있고, 해당 회사 소속으로만 로그인됩니다."
        );
    }

    @Transactional
    public LoginResponse activateInvite(InviteActivateRequest request) {
        EmployeeInvite invite = getValidInvite(request.getInviteToken());
        Employee employee = invite.getEmployee();
        String normalizedDeviceId = request.getDeviceId().trim();
        String normalizedDeviceName = request.getDeviceName() == null ? null : request.getDeviceName().trim();

        employee.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        employee.markPasswordChanged();
        employee.registerDevice(normalizedDeviceId, normalizedDeviceName);
        invite.markUsed();

        String token = jwtTokenProvider.generateToken(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getCompany().getId(),
            normalizedDeviceId
        );
        String accessTokenExpiresAt = ISO_DATE_TIME_FORMATTER.format(jwtTokenProvider.getExpiration(token));

        return new LoginResponse(
            token,
            "Bearer",
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getCompany().getId(),
            employee.getCompany().getName(),
            employee.getWorkplace() == null ? null : employee.getWorkplace().getName(),
            employee.getRole().name(),
            false,
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

    private EmployeeInvite getValidInvite(String rawToken) {
        EmployeeInvite invite = employeeInviteRepository.findByToken(rawToken.trim())
            .orElseThrow(() -> new UnauthorizedException("유효하지 않은 초대 링크입니다."));

        if (!invite.hasRemainingUses(MAX_INVITE_USES)) {
            throw new UnauthorizedException("사용 가능 횟수를 모두 소진한 초대 링크입니다.");
        }

        if (invite.isExpired(LocalDateTime.now())) {
            throw new UnauthorizedException("만료된 초대 링크입니다.");
        }

        Employee employee = invite.getEmployee();
        if (employee.isDeleted() || !employee.isActive()) {
            throw new UnauthorizedException("사용할 수 없는 초대 링크입니다.");
        }

        return invite;
    }
}
