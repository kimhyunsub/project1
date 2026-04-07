package com.attendance.backend.service;

import com.attendance.backend.config.InviteProperties;
import com.attendance.backend.domain.entity.AttendanceRecord;
import com.attendance.backend.domain.entity.Company;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.EmployeeInvite;
import com.attendance.backend.domain.entity.EmployeeRole;
import com.attendance.backend.domain.entity.Workplace;
import com.attendance.backend.domain.repository.AttendanceRecordRepository;
import com.attendance.backend.domain.repository.CompanyRepository;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import com.attendance.backend.domain.repository.EmployeeInviteRepository;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.domain.repository.WorkplaceRepository;
import com.attendance.backend.dto.admin.CompanySettingResponse;
import com.attendance.backend.dto.admin.CreateEmployeeInviteRequest;
import com.attendance.backend.dto.admin.CreateEmployeeInviteResponse;
import com.attendance.backend.dto.admin.CreateWorkplaceRequest;
import com.attendance.backend.dto.admin.EmployeeSummaryResponse;
import com.attendance.backend.dto.admin.TodayAttendanceOverviewResponse;
import com.attendance.backend.dto.admin.UpdateAttendanceRadiusRequest;
import com.attendance.backend.dto.admin.UpdateCompanyLocationRequest;
import com.attendance.backend.dto.admin.WorkplaceResponse;
import com.attendance.backend.dto.internal.InternalAttendanceRowResponse;
import com.attendance.backend.dto.internal.InternalDashboardResponse;
import com.attendance.backend.dto.internal.InternalDashboardSummaryResponse;
import com.attendance.backend.dto.internal.InternalEmployeeFormResponse;
import com.attendance.backend.dto.internal.InternalEmployeeInviteCreateRequest;
import com.attendance.backend.dto.internal.InternalEmployeeInviteResponse;
import com.attendance.backend.dto.internal.InternalEmployeePageResponse;
import com.attendance.backend.dto.internal.InternalEmployeeRowResponse;
import com.attendance.backend.dto.internal.InternalEmployeeUploadResponse;
import com.attendance.backend.dto.internal.InternalEmployeeUpsertRequest;
import com.attendance.backend.dto.internal.InternalCompanyLocationUpdateRequest;
import com.attendance.backend.dto.internal.InternalAdminUserDetailsResponse;
import com.attendance.backend.dto.internal.InternalLocationSettingsResponse;
import com.attendance.backend.dto.internal.InternalMonthlyAttendanceEmployeeDetailResponse;
import com.attendance.backend.dto.internal.InternalMonthlyAttendanceEmployeeRowResponse;
import com.attendance.backend.dto.internal.InternalMonthlyAttendanceRecordRowResponse;
import com.attendance.backend.dto.internal.InternalMonthlyAttendanceResponse;
import com.attendance.backend.dto.internal.InternalMonthlyAttendanceSummaryResponse;
import com.attendance.backend.dto.internal.InternalWorkplaceLocationResponse;
import com.attendance.backend.dto.internal.InternalWorkplaceUpsertRequest;
import com.attendance.backend.dto.internal.InternalSqlQueryResultResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.exception.ResourceNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월");
    private static final int SQL_PREVIEW_ROW_LIMIT = 200;
    private static final int SQL_EXPORT_ROW_LIMIT = 5000;
    private static final int EXCEL_CELL_TEXT_LIMIT = 32767;
    private static final DataFormatter EXCEL_DATA_FORMATTER = new DataFormatter();

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingRepository companySettingRepository;
    private final EmployeeInviteRepository employeeInviteRepository;
    private final WorkplaceRepository workplaceRepository;
    private final AttendanceExcelService attendanceExcelService;
    private final InviteProperties inviteProperties;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    public AdminService(
        EmployeeRepository employeeRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        CompanyRepository companyRepository,
        CompanySettingRepository companySettingRepository,
        EmployeeInviteRepository employeeInviteRepository,
        WorkplaceRepository workplaceRepository,
        AttendanceExcelService attendanceExcelService,
        InviteProperties inviteProperties,
        PasswordEncoder passwordEncoder,
        DataSource dataSource
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.companyRepository = companyRepository;
        this.companySettingRepository = companySettingRepository;
        this.employeeInviteRepository = employeeInviteRepository;
        this.workplaceRepository = workplaceRepository;
        this.attendanceExcelService = attendanceExcelService;
        this.inviteProperties = inviteProperties;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    public List<EmployeeSummaryResponse> getEmployees(Long adminEmployeeId) {
        Employee admin = getEmployee(adminEmployeeId);
        return employeeRepository.findAllByCompanyIdAndDeletedFalseOrderByNameAsc(admin.getCompany().getId()).stream()
            .map(employee -> new EmployeeSummaryResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getName(),
                employee.getRole().name(),
                employee.getCompany().getName()
            ))
            .toList();
    }

    public InternalAdminUserDetailsResponse getAdminUserDetails(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeCode(employeeCode)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        if (employee.getRole() != EmployeeRole.ADMIN && employee.getRole() != EmployeeRole.WORKPLACE_ADMIN) {
            throw new BusinessException("관리자 권한 계정만 로그인할 수 있습니다.");
        }

        if (employee.isDeleted()) {
            throw new BusinessException("삭제된 계정입니다.");
        }

        return new InternalAdminUserDetailsResponse(
            employee.getEmployeeCode(),
            employee.getPassword(),
            employee.getRole().name(),
            employee.isActive()
        );
    }

    @Transactional
    public CreateEmployeeInviteResponse createEmployeeInvite(Long adminEmployeeId, CreateEmployeeInviteRequest request) {
        Employee admin = getEmployee(adminEmployeeId);
        Company company = companyRepository.findById(admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));
        assertCanAddEmployee(company);
        String employeeCode = request.getEmployeeCode().trim();
        String employeeName = request.getEmployeeName().trim();

        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new BusinessException("이미 사용 중인 직원 아이디입니다.");
        }

        EmployeeRole role = parseRole(request.getRole());
        Workplace workplace = resolveWorkplace(company.getId(), request.getWorkplaceId(), role);

        Employee employee = employeeRepository.save(
            new Employee(
                employeeCode,
                employeeName,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                role,
                company,
                workplace,
                null,
                null
            )
        );
        employee.markPasswordChangeRequired();

        String inviteToken = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(inviteProperties.getExpirationHours());
        EmployeeInvite invite = employeeInviteRepository.save(new EmployeeInvite(inviteToken, employee, expiresAt));

        return new CreateEmployeeInviteResponse(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getRole().name(),
            company.getId(),
            company.getName(),
            workplace == null ? null : workplace.getId(),
            workplace == null ? null : workplace.getName(),
            invite.getToken(),
            buildInviteUrl(invite.getToken()),
            expiresAt.toString(),
            "직원 초대 링크가 생성되었습니다. 이 링크로 앱을 열면 해당 회사 소속으로만 활성화됩니다."
        );
    }

    @Transactional
    public InternalEmployeeInviteResponse createEmployeeInviteForAdmin(String adminEmployeeCode, Long employeeId) {
        Employee employee = getEditableEmployee(adminEmployeeCode, employeeId);

        if (employee.isDeleted()) {
            throw new BusinessException("삭제된 직원은 초대 링크를 생성할 수 없습니다.");
        }

        if (employee.getRole() == EmployeeRole.ADMIN) {
            throw new BusinessException("관리자 계정은 목록에서 초대 링크를 생성할 수 없습니다.");
        }

        String inviteToken = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(inviteProperties.getExpirationHours());
        employeeInviteRepository.save(new EmployeeInvite(inviteToken, employee, expiresAt));

        return new InternalEmployeeInviteResponse(
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getRole().name(),
            employee.getWorkplace() == null ? "본사" : employee.getWorkplace().getName(),
            buildInviteUrl(inviteToken),
            expiresAt.toString(),
            "직원 초대 링크가 생성되었습니다. 팝업에서 바로 복사해 전달할 수 있습니다."
        );
    }

    @Transactional
    public InternalEmployeeInviteResponse createEmployeeInviteForAdmin(String adminEmployeeCode, InternalEmployeeInviteCreateRequest request) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        assertCanAddEmployee(admin.getCompany());
        EmployeeRole role = parseRole(request.getRole());
        validateRoleAssignment(admin, role, request.getWorkplaceId());
        validateDuplicateEmployeeCode(request.getEmployeeCode(), null);
        Workplace workplace = resolveManagedWorkplace(admin, request.getWorkplaceId());

        Employee employee = new Employee(
            request.getEmployeeCode().trim(),
            request.getName().trim(),
            passwordEncoder.encode(UUID.randomUUID().toString()),
            role,
            admin.getCompany(),
            workplace,
            null,
            null
        );
        employee.markPasswordChangeRequired();
        employeeRepository.save(employee);

        String inviteToken = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(inviteProperties.getExpirationHours());
        employeeInviteRepository.save(new EmployeeInvite(inviteToken, employee, expiresAt));

        return new InternalEmployeeInviteResponse(
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getRole().name(),
            workplace == null ? "본사" : workplace.getName(),
            buildInviteUrl(inviteToken),
            expiresAt.toString(),
            "직원 초대 링크가 생성되었습니다. 팝업에서 바로 복사해 전달할 수 있습니다."
        );
    }

    public List<TodayAttendanceOverviewResponse> getTodayAttendance(Long adminEmployeeId) {
        Employee admin = getEmployee(adminEmployeeId);
        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);

        List<Employee> employees = employeeRepository.findAllByCompanyIdAndDeletedFalseOrderByNameAsc(admin.getCompany().getId());
        Map<Long, AttendanceRecord> recordsByEmployeeId =
            attendanceRecordRepository.findAllByEmployeeCompanyIdAndAttendanceDate(admin.getCompany().getId(), today)
                .stream()
                .collect(Collectors.toMap(
                    record -> record.getEmployee().getId(),
                    Function.identity(),
                    this::pickLatestAttendanceRecord
                ));

        return employees.stream()
            .map(employee -> {
                AttendanceRecord record = recordsByEmployeeId.get(employee.getId());
                return new TodayAttendanceOverviewResponse(
                    employee.getId(),
                    employee.getEmployeeCode(),
                    employee.getName(),
                    today,
                    record != null,
                    record != null ? record.getCheckInTime() : null,
                    record != null ? record.getCheckOutTime() : null,
                    record != null && record.isLate(),
                    record != null ? record.getStatus().name() : "ABSENT"
                );
            })
            .toList();
    }

    public byte[] exportMonthlyAttendanceExcel(Long adminEmployeeId, int year, int month) {
        Employee admin = getEmployee(adminEmployeeId);
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, month);
        } catch (DateTimeException ex) {
            throw new BusinessException("year 또는 month 값이 올바르지 않습니다.");
        }
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Employee> employees = employeeRepository.findAllByCompanyIdAndDeletedFalseOrderByNameAsc(admin.getCompany().getId());
        List<AttendanceRecord> records = attendanceRecordRepository.findAllByEmployeeCompanyIdAndAttendanceDateBetween(
            admin.getCompany().getId(),
            startDate,
            endDate
        );

        return attendanceExcelService.createMonthlyAttendanceWorkbook(yearMonth, employees, records);
    }

    @Transactional
    public CompanySettingResponse updateCompanyLocation(Long adminEmployeeId, UpdateCompanyLocationRequest request) {
        Employee admin = getEmployee(adminEmployeeId);
        Company company = companyRepository.findById(admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));

        company.updateLocation(request.getLatitude(), request.getLongitude());
        CompanySetting setting = getCompanySetting(company);

        return new CompanySettingResponse(
            company.getId(),
            company.getName(),
            null,
            null,
            company.getLatitude(),
            company.getLongitude(),
            setting.getAllowedRadiusMeters(),
            setting.getLateAfterTime(),
            setting.getNoticeMessage(),
            setting.getMobileSkinKey(),
            setting.isEnforceSingleDeviceLogin(),
            "회사 위치가 수정되었습니다."
        );
    }

    @Transactional
    public CompanySettingResponse updateAttendanceRadius(Long adminEmployeeId, UpdateAttendanceRadiusRequest request) {
        Employee admin = getEmployee(adminEmployeeId);
        Company company = companyRepository.findById(admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));
        CompanySetting setting = getCompanySetting(company);

        setting.updateAllowedRadiusMeters(request.getAllowedRadiusMeters());

        return new CompanySettingResponse(
            company.getId(),
            company.getName(),
            null,
            null,
            company.getLatitude(),
            company.getLongitude(),
            setting.getAllowedRadiusMeters(),
            setting.getLateAfterTime(),
            setting.getNoticeMessage(),
            setting.getMobileSkinKey(),
            setting.isEnforceSingleDeviceLogin(),
            "출근 반경이 수정되었습니다."
        );
    }

    @Transactional
    public WorkplaceResponse createWorkplace(Long adminEmployeeId, CreateWorkplaceRequest request) {
        Employee admin = getEmployee(adminEmployeeId);
        Company company = companyRepository.findById(admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));
        assertCanAddWorkplace(company);
        String workplaceName = request.getName().trim();

        if (workplaceRepository.existsByCompanyIdAndName(company.getId(), workplaceName)) {
            throw new BusinessException("이미 등록된 사업장명입니다.");
        }

        Workplace workplace = workplaceRepository.save(
            new Workplace(
                company,
                workplaceName,
                request.getLatitude(),
                request.getLongitude(),
                request.getAllowedRadiusMeters(),
                request.getNoticeMessage()
            )
        );

        return new WorkplaceResponse(
            company.getId(),
            company.getName(),
            workplace.getId(),
            workplace.getName(),
            workplace.getLatitude(),
            workplace.getLongitude(),
            workplace.getAllowedRadiusMeters(),
            workplace.getNoticeMessage(),
            "사업장이 추가되었습니다."
        );
    }

    public InternalLocationSettingsResponse getLocationSettingsForAdmin(String adminEmployeeCode) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        Company company = companyRepository.findById(admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));
        CompanySetting setting = getCompanySetting(company);

        List<InternalWorkplaceLocationResponse> workplaces = getAccessibleWorkplaces(admin).stream()
            .map(workplace -> new InternalWorkplaceLocationResponse(
                workplace.getId(),
                workplace.getName(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                workplace.getAllowedRadiusMeters(),
                workplace.getNoticeMessage()
            ))
            .toList();

        return new InternalLocationSettingsResponse(
            company.getName(),
            company.getLatitude(),
            company.getLongitude(),
            setting.getAllowedRadiusMeters(),
            setting.getLateAfterTime(),
            setting.getNoticeMessage(),
            normalizeMobileSkinKey(setting.getMobileSkinKey()),
            setting.isEnforceSingleDeviceLogin(),
            isWorkplaceScopedAdmin(admin),
            getAssignedWorkplaceId(admin),
            workplaces
        );
    }

    public InternalSqlQueryResultResponse executeReadOnlySqlForAdmin(String adminEmployeeCode, String queryText) {
        Employee admin = requireSqlConsoleAdmin(adminEmployeeCode);
        return runSqlQuery(buildExecutableSql(admin, validateSqlQuery(admin, queryText)), admin, SQL_PREVIEW_ROW_LIMIT);
    }

    public byte[] exportSqlQueryExcelForAdmin(String adminEmployeeCode, String queryText) {
        Employee admin = requireSqlConsoleAdmin(adminEmployeeCode);
        InternalSqlQueryResultResponse queryResult = runSqlQuery(
            buildExecutableSql(admin, validateSqlQuery(admin, queryText)),
            admin,
            SQL_EXPORT_ROW_LIMIT
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor((short) 22);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet sheet = workbook.createSheet("SQL 결과");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < queryResult.columns().size(); index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(queryResult.columns().get(index));
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < queryResult.rows().size(); rowIndex++) {
                List<String> rowValues = queryResult.rows().get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < rowValues.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(trimExcelCellValue(rowValues.get(columnIndex)));
                }
            }

            for (int index = 0; index < queryResult.columns().size(); index++) {
                sheet.autoSizeColumn(index);
                sheet.setColumnWidth(index, Math.max(sheet.getColumnWidth(index), 3600));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("SQL 결과 엑셀을 생성할 수 없습니다.", exception);
        }
    }

    @Transactional
    public void updateCompanySettingsForAdmin(String adminEmployeeCode, InternalCompanyLocationUpdateRequest request) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        if (isWorkplaceScopedAdmin(admin)) {
            throw new BusinessException("사업장 관리자는 회사 기본 설정을 수정할 수 없습니다.");
        }

        Company company = companyRepository.findById(admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));
        CompanySetting setting = getCompanySetting(company);

        company.updateName(request.getCompanyName().trim());
        company.updateLocation(request.getLatitude(), request.getLongitude());
        setting.updateAllowedRadiusMeters(request.getAllowedRadiusMeters());
        setting.updateNoticeMessage(normalizeNoticeMessage(request.getNoticeMessage()));
        setting.updateMobileSkinKey(normalizeMobileSkinKey(request.getMobileSkinKey()));
        setting.updateEnforceSingleDeviceLogin(request.isEnforceSingleDeviceLogin());
    }

    @Transactional
    public void createWorkplaceForAdmin(String adminEmployeeCode, InternalWorkplaceUpsertRequest request) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        if (isWorkplaceScopedAdmin(admin)) {
            throw new BusinessException("사업장 관리자는 사업장을 추가할 수 없습니다.");
        }

        Company company = companyRepository.findById(admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));
        assertCanAddWorkplace(company);
        String workplaceName = request.getName().trim();

        if (workplaceRepository.existsByCompanyIdAndName(company.getId(), workplaceName)) {
            throw new BusinessException("이미 등록된 사업장명입니다.");
        }

        workplaceRepository.save(new Workplace(
            company,
            workplaceName,
            request.getLatitude(),
            request.getLongitude(),
            request.getAllowedRadiusMeters(),
            normalizeNoticeMessage(request.getNoticeMessage())
        ));
    }

    @Transactional
    public void updateWorkplaceForAdmin(String adminEmployeeCode, Long workplaceId, InternalWorkplaceUpsertRequest request) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        Workplace workplace = getManagedWorkplace(admin, workplaceId);
        String workplaceName = request.getName().trim();

        if (workplaceRepository.existsByCompanyIdAndNameAndIdNot(admin.getCompany().getId(), workplaceName, workplaceId)) {
            throw new BusinessException("이미 등록된 사업장명입니다.");
        }

        workplace.update(
            workplaceName,
            request.getLatitude(),
            request.getLongitude(),
            request.getAllowedRadiusMeters(),
            normalizeNoticeMessage(request.getNoticeMessage())
        );
    }

    public InternalEmployeePageResponse getEmployeePageForAdmin(
        String adminEmployeeCode,
        boolean showDeleted,
        Long workplaceId,
        int page,
        int pageSize
    ) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        List<Employee> employees = getEmployeeList(admin, showDeleted, workplaceId);
        Map<Long, AttendanceRecord> recordsByEmployeeId = attendanceRecordRepository
            .findAllByEmployeeCompanyIdAndAttendanceDate(admin.getCompany().getId(), LocalDate.now(SEOUL_ZONE_ID))
            .stream()
            .collect(Collectors.toMap(
                record -> record.getEmployee().getId(),
                Function.identity(),
                this::pickLatestAttendanceRecord
            ));

        List<InternalEmployeeRowResponse> rows = employees.stream()
            .map(employee -> {
                AttendanceRecord record = recordsByEmployeeId.get(employee.getId());
                return new InternalEmployeeRowResponse(
                    employee.getId(),
                    employee.getEmployeeCode(),
                    employee.getName(),
                    employee.getWorkplace() == null ? "본사" : employee.getWorkplace().getName(),
                    employee.getRole().name(),
                    formatTime(employee.getWorkStartTime()),
                    formatTime(employee.getWorkEndTime()),
                    toAttendanceState(record),
                    formatDateTime(record == null ? null : record.getCheckInTime()),
                    formatDateTime(record == null ? null : record.getCheckOutTime()),
                    employee.hasRegisteredDevice(),
                    employee.isActive(),
                    employee.isDeleted()
                );
            })
            .toList();

        int normalizedPageSize = Math.max(pageSize, 1);
        int totalCount = rows.size();
        int totalPages = totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / normalizedPageSize);
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * normalizedPageSize, totalCount);
        int toIndex = Math.min(fromIndex + normalizedPageSize, totalCount);

        return new InternalEmployeePageResponse(
            rows.subList(fromIndex, toIndex),
            currentPage,
            totalPages,
            totalCount,
            normalizedPageSize,
            currentPage > 1,
            currentPage < totalPages
        );
    }

    public InternalDashboardResponse getTodayDashboardForAdmin(String adminEmployeeCode, String filter, Long workplaceId) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        List<Employee> employees = getEmployeeList(admin, false, workplaceId);
        Map<Long, AttendanceRecord> recordsByEmployeeId = attendanceRecordRepository
            .findAllByEmployeeCompanyIdAndAttendanceDate(admin.getCompany().getId(), LocalDate.now(SEOUL_ZONE_ID))
            .stream()
            .collect(Collectors.toMap(
                record -> record.getEmployee().getId(),
                Function.identity(),
                this::pickLatestAttendanceRecord
            ));

        int total = employees.size();
        int present = 0;
        int late = 0;
        int absent = 0;
        int checkedOut = 0;

        for (Employee employee : employees) {
            AttendanceRecord record = recordsByEmployeeId.get(employee.getId());
            String state = toAttendanceState(record);
            switch (state) {
                case "WORKING" -> present++;
                case "LATE" -> late++;
                case "CHECKED_OUT" -> checkedOut++;
                default -> absent++;
            }
        }

        List<InternalAttendanceRowResponse> rows = employees.stream()
            .map(employee -> {
                AttendanceRecord record = recordsByEmployeeId.get(employee.getId());
                return new InternalAttendanceRowResponse(
                    employee.getEmployeeCode(),
                    employee.getName(),
                    employee.getWorkplace() == null ? "본사" : employee.getWorkplace().getName(),
                    employee.getRole().name(),
                    toAttendanceState(record),
                    formatDateTime(record == null ? null : record.getCheckInTime()),
                    formatDateTime(record == null ? null : record.getCheckOutTime()),
                    buildAttendanceNote(record)
                );
            })
            .filter(row -> matchesDashboardFilter(row.state(), filter))
            .toList();

        return new InternalDashboardResponse(
            new InternalDashboardSummaryResponse(total, present, late, absent, checkedOut),
            rows
        );
    }

    public InternalMonthlyAttendanceResponse getMonthlyAttendanceForAdmin(
        String adminEmployeeCode,
        int year,
        int month,
        String selectedEmployeeCode,
        Long workplaceId
    ) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, month);
        } catch (DateTimeException ex) {
            throw new BusinessException("year 또는 month 값이 올바르지 않습니다.");
        }

        List<Employee> employees = getEmployeeList(admin, false, workplaceId);
        Map<Long, List<AttendanceRecord>> recordsByEmployeeId = attendanceRecordRepository
            .findAllByEmployeeCompanyIdAndAttendanceDateBetween(
                admin.getCompany().getId(),
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
            )
            .stream()
            .filter(record -> !record.getEmployee().isDeleted())
            .filter(record -> employees.stream().anyMatch(employee -> employee.getId().equals(record.getEmployee().getId())))
            .collect(Collectors.groupingBy(record -> record.getEmployee().getId()));

        int totalEmployees = employees.size();
        int attendedEmployees = 0;
        int attendanceCount = 0;
        int lateCount = 0;
        int checkedOutCount = 0;

        List<InternalMonthlyAttendanceEmployeeRowResponse> employeeRows = employees.stream()
            .map(employee -> {
                List<AttendanceRecord> records = recordsByEmployeeId.getOrDefault(employee.getId(), List.of());
                AttendanceRecord lastRecord = records.stream()
                    .max(Comparator.comparing(AttendanceRecord::getAttendanceDate).thenComparing(AttendanceRecord::getCheckInTime))
                    .orElse(null);

                int employeeLateDays = (int) records.stream().filter(AttendanceRecord::isLate).count();
                int employeeCheckedOutDays = (int) records.stream().filter(record -> record.getCheckOutTime() != null).count();

                return new InternalMonthlyAttendanceEmployeeRowResponse(
                    employee.getEmployeeCode(),
                    employee.getName(),
                    employee.getWorkplace() == null ? "본사" : employee.getWorkplace().getName(),
                    employee.getRole().name(),
                    records.size(),
                    employeeLateDays,
                    employeeCheckedOutDays,
                    lastRecord == null ? "-" : lastRecord.getAttendanceDate().format(DATE_FORMATTER),
                    lastRecord == null ? "ABSENT" : toAttendanceState(lastRecord)
                );
            })
            .toList();

        for (InternalMonthlyAttendanceEmployeeRowResponse row : employeeRows) {
            if (row.attendanceDays() > 0) {
                attendedEmployees++;
            }
            attendanceCount += row.attendanceDays();
            lateCount += row.lateDays();
            checkedOutCount += row.checkedOutDays();
        }

        List<InternalMonthlyAttendanceRecordRowResponse> recordRows = recordsByEmployeeId.values().stream()
            .flatMap(List::stream)
            .sorted(Comparator.comparing(AttendanceRecord::getAttendanceDate).reversed()
                .thenComparing(AttendanceRecord::getCheckInTime, Comparator.reverseOrder()))
            .map(record -> new InternalMonthlyAttendanceRecordRowResponse(
                record.getAttendanceDate().format(DATE_FORMATTER),
                record.getEmployee().getEmployeeCode(),
                record.getEmployee().getName(),
                record.getEmployee().getWorkplace() == null ? "본사" : record.getEmployee().getWorkplace().getName(),
                record.getEmployee().getRole().name(),
                toAttendanceState(record),
                formatDateTime(record.getCheckInTime()),
                formatDateTime(record.getCheckOutTime()),
                buildAttendanceNote(record)
            ))
            .toList();

        InternalMonthlyAttendanceEmployeeDetailResponse detailResponse = buildMonthlyEmployeeDetailResponse(
            employees,
            recordsByEmployeeId,
            selectedEmployeeCode
        );

        return new InternalMonthlyAttendanceResponse(
            new InternalMonthlyAttendanceSummaryResponse(
                yearMonth.format(MONTH_FORMATTER),
                totalEmployees,
                attendedEmployees,
                attendanceCount,
                lateCount,
                checkedOutCount
            ),
            employeeRows,
            recordRows,
            detailResponse
        );
    }

    public InternalEmployeeFormResponse getEmployeeFormForAdmin(String adminEmployeeCode, Long employeeId) {
        Employee employee = getEditableEmployee(adminEmployeeCode, employeeId);
        if (employee.isDeleted()) {
            throw new BusinessException("삭제된 직원은 수정할 수 없습니다. 먼저 복구해 주세요.");
        }

        return new InternalEmployeeFormResponse(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getRole().name(),
            formatTime(employee.getWorkStartTime()),
            formatTime(employee.getWorkEndTime()),
            employee.getWorkplace() == null ? null : employee.getWorkplace().getId()
        );
    }

    @Transactional
    public void createEmployeeForAdmin(String adminEmployeeCode, InternalEmployeeUpsertRequest request) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        assertCanAddEmployee(admin.getCompany());
        EmployeeRole role = parseManageableRole(request.getRole());
        validateRoleAssignment(admin, role, request.getWorkplaceId());
        validateDuplicateEmployeeCode(request.getEmployeeCode(), null);
        Workplace workplace = resolveManagedWorkplace(admin, request.getWorkplaceId());

        String encodedPassword = role == EmployeeRole.EMPLOYEE
            ? passwordEncoder.encode(UUID.randomUUID().toString())
            : passwordEncoder.encode(request.getPassword());

        Employee employee = new Employee(
            request.getEmployeeCode().trim(),
            request.getName().trim(),
            encodedPassword,
            role,
            admin.getCompany(),
            workplace,
            parseOptionalTime(request.getWorkStartTime(), "출근 기준 시간"),
            parseOptionalTime(request.getWorkEndTime(), "퇴근 기준 시간")
        );
        applyPasswordChangePolicy(employee, role);
        employeeRepository.save(employee);
    }

    @Transactional
    public void updateEmployeeForAdmin(String adminEmployeeCode, Long employeeId, InternalEmployeeUpsertRequest request) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        Employee employee = getEditableEmployee(adminEmployeeCode, employeeId);
        if (employee.isDeleted()) {
            throw new BusinessException("삭제된 직원은 수정할 수 없습니다. 먼저 복구해 주세요.");
        }

        EmployeeRole role = parseManageableRole(request.getRole());
        validateRoleAssignment(admin, role, request.getWorkplaceId());
        validateDuplicateEmployeeCode(request.getEmployeeCode(), employeeId);
        Workplace workplace = resolveManagedWorkplace(admin, request.getWorkplaceId());

        employee.updateProfile(
            request.getEmployeeCode().trim(),
            request.getName().trim(),
            role,
            workplace,
            parseOptionalTime(request.getWorkStartTime(), "출근 기준 시간"),
            parseOptionalTime(request.getWorkEndTime(), "퇴근 기준 시간")
        );

        if (StringUtils.hasText(request.getPassword())) {
            employee.updatePassword(passwordEncoder.encode(request.getPassword()));
            applyPasswordChangePolicy(employee, role);
        }
    }

    @Transactional
    public void updateEmployeeUsageForAdmin(String adminEmployeeCode, Long employeeId, boolean active) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        Employee employee = getEditableEmployee(adminEmployeeCode, employeeId);

        if (employee.isDeleted()) {
            throw new BusinessException("삭제된 직원은 사용 여부를 변경할 수 없습니다. 먼저 복구해 주세요.");
        }

        if (admin.getId().equals(employee.getId())) {
            throw new BusinessException("현재 로그인한 관리자 계정은 사용 중지할 수 없습니다.");
        }

        if (employee.isActive() == active) {
            throw new BusinessException(active ? "이미 사용 중인 직원입니다." : "이미 사용 중지된 직원입니다.");
        }

        employee.updateActive(active);
    }

    @Transactional
    public void resetEmployeeDeviceForAdmin(String adminEmployeeCode, Long employeeId) {
        Employee employee = getEditableEmployee(adminEmployeeCode, employeeId);
        if (employee.isDeleted()) {
            throw new BusinessException("삭제된 직원의 단말은 초기화할 수 없습니다.");
        }
        employee.resetRegisteredDevice();
    }

    @Transactional
    public void deleteEmployeeForAdmin(String adminEmployeeCode, Long employeeId) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        Employee employee = getEditableEmployee(adminEmployeeCode, employeeId);

        if (admin.getId().equals(employee.getId())) {
            throw new BusinessException("현재 로그인한 관리자 계정은 삭제할 수 없습니다.");
        }

        if (employee.isDeleted()) {
            throw new BusinessException("이미 삭제된 직원입니다.");
        }

        employee.softDelete();
    }

    @Transactional
    public void restoreEmployeeForAdmin(String adminEmployeeCode, Long employeeId) {
        Employee employee = getEditableEmployee(adminEmployeeCode, employeeId);

        if (!employee.isDeleted()) {
            throw new BusinessException("삭제된 직원만 복구할 수 있습니다.");
        }

        employee.restore();
    }

    @Transactional
    public InternalEmployeeUploadResponse uploadEmployeesForAdmin(String adminEmployeeCode, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("업로드할 엑셀 파일을 선택해 주세요.");
        }

        Employee admin = getEmployeeByCode(adminEmployeeCode);
        Set<String> existingCodes = employeeRepository.findAllByCompanyIdOrderByNameAsc(admin.getCompany().getId()).stream()
            .map(Employee::getEmployeeCode)
            .collect(Collectors.toCollection(HashSet::new));
        Map<String, Workplace> workplacesByName = workplaceRepository.findAllByCompanyIdOrderByNameAsc(admin.getCompany().getId()).stream()
            .collect(Collectors.toMap(
                workplace -> workplace.getName().trim().toLowerCase(),
                Function.identity(),
                (first, second) -> first
            ));
        List<String> failureMessages = new ArrayList<>();
        int successCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("엑셀 시트가 비어 있습니다.");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row)) {
                    continue;
                }

                String employeeCode = readCell(row, 0);
                String name = readCell(row, 1);
                String roleValue = readCell(row, 2).toUpperCase();
                String password = readCell(row, 3);
                String workplaceName = readCell(row, 4);
                String workStartTime = readCell(row, 5);
                String workEndTime = readCell(row, 6);

                try {
                    validateUploadRow(rowIndex + 1, employeeCode, name, roleValue, password, workplaceName, workStartTime, workEndTime, existingCodes);
                    validateUploadRole(admin, rowIndex + 1, roleValue);
                    assertCanAddEmployee(admin.getCompany());
                    Workplace workplace = resolveUploadWorkplace(admin, rowIndex + 1, workplaceName, workplacesByName);
                    Employee employee = new Employee(
                        employeeCode,
                        name,
                        passwordEncoder.encode(password),
                        EmployeeRole.valueOf(roleValue),
                        admin.getCompany(),
                        workplace,
                        parseOptionalTime(workStartTime, rowIndex + 1 + "행 출근 기준 시간"),
                        parseOptionalTime(workEndTime, rowIndex + 1 + "행 퇴근 기준 시간")
                    );
                    applyPasswordChangePolicy(employee, employee.getRole());
                    employeeRepository.saveAndFlush(employee);
                    existingCodes.add(employeeCode);
                    successCount++;
                } catch (BusinessException exception) {
                    failureMessages.add(exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new BusinessException("엑셀 파일을 읽는 중 오류가 발생했습니다.");
        } catch (RuntimeException exception) {
            throw new BusinessException("지원하지 않는 엑셀 파일 형식입니다. .xlsx 파일을 사용해 주세요.");
        }

        return new InternalEmployeeUploadResponse(successCount, failureMessages.size(), failureMessages);
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private Employee getEmployeeByCode(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeCode(employeeCode)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        if (employee.getRole() != EmployeeRole.ADMIN && employee.getRole() != EmployeeRole.WORKPLACE_ADMIN) {
            throw new BusinessException("관리자 또는 사업장 관리자만 접근할 수 있습니다.");
        }
        return employee;
    }

    private CompanySetting getCompanySetting(Company company) {
        return companySettingRepository.findByCompany(company)
            .orElseThrow(() -> new ResourceNotFoundException("회사 설정을 찾을 수 없습니다."));
    }

    private void assertCanAddEmployee(Company company) {
        Integer employeeLimit = company.getEmployeeLimit();
        if (employeeLimit == null) {
            return;
        }

        long currentEmployees = employeeRepository.countByCompanyIdAndDeletedFalseAndRoleNot(
            company.getId(),
            EmployeeRole.ADMIN
        );
        if (currentEmployees >= employeeLimit) {
            throw new BusinessException("직원 추가 한도(" + employeeLimit + "명)를 초과했습니다. 플랫폼 관리자에게 등급 또는 제한값 조정을 요청해 주세요.");
        }
    }

    private List<Employee> getEmployeeList(Employee admin, boolean showDeleted, Long workplaceId) {
        return employeeRepository.findAllByCompanyIdOrderByNameAsc(admin.getCompany().getId()).stream()
            .filter(employee -> showDeleted || !employee.isDeleted())
            .filter(employee -> matchesWorkplaceScope(admin, employee))
            .filter(employee -> matchesRequestedWorkplace(employee, resolveRequestedWorkplaceId(admin, workplaceId)))
            .toList();
    }

    private void assertCanAddWorkplace(Company company) {
        Integer workplaceLimit = company.getWorkplaceLimit();
        if (workplaceLimit == null) {
            return;
        }

        long currentWorkplaces = workplaceRepository.countByCompanyId(company.getId());
        if (currentWorkplaces >= workplaceLimit) {
            throw new BusinessException("사업장 추가 한도(" + workplaceLimit + "개)를 초과했습니다. 플랫폼 관리자에게 등급 또는 제한값 조정을 요청해 주세요.");
        }
    }

    private Employee requireSqlConsoleAdmin(String employeeCode) {
        return getEmployeeByCode(employeeCode);
    }

    private String validateSqlQuery(Employee admin, String queryText) {
        if (!StringUtils.hasText(queryText)) {
            throw new BusinessException("실행할 SQL을 입력해 주세요.");
        }

        String normalized = queryText.trim();
        String lowerCaseQuery = normalized.toLowerCase();

        if (normalized.length() > 20000) {
            throw new BusinessException("SQL은 20,000자 이하로 입력해 주세요.");
        }
        if (normalized.contains(";")) {
            throw new BusinessException("세미콜론 없이 단일 조회 쿼리만 실행할 수 있습니다.");
        }
        if (lowerCaseQuery.contains("--") || lowerCaseQuery.contains("/*") || lowerCaseQuery.contains("*/")) {
            throw new BusinessException("주석이 포함된 SQL은 실행할 수 없습니다.");
        }
        if (!(lowerCaseQuery.startsWith("select") || lowerCaseQuery.startsWith("with"))) {
            throw new BusinessException("SELECT 또는 WITH로 시작하는 조회 쿼리만 실행할 수 있습니다.");
        }

        String[] blockedKeywords = {
            "insert", "update", "delete", "merge", "drop", "alter", "truncate",
            "create", "grant", "revoke", "comment", "call", "execute", "exec",
            "vacuum", "analyze", "refresh", "copy", "set"
        };

        for (String blockedKeyword : blockedKeywords) {
            if (lowerCaseQuery.matches("(?s).*\\b" + blockedKeyword + "\\b.*")) {
                throw new BusinessException("조회 전용 SQL만 실행할 수 있습니다. 금지된 키워드: " + blockedKeyword.toUpperCase());
            }
        }

        if (isWorkplaceScopedAdmin(admin)) {
            if (!lowerCaseQuery.matches("(?s).*\\bscoped_(employees|attendance_records|workplace)\\b.*")) {
                throw new BusinessException("사업장 관리자는 scoped_employees, scoped_attendance_records, scoped_workplace 중 하나를 사용해 조회해 주세요.");
            }

            String[] blockedTableNames = {"employees", "attendance_records", "workplaces", "companies", "company_settings"};
            for (String blockedTableName : blockedTableNames) {
                if (lowerCaseQuery.matches("(?s).*\\b" + blockedTableName + "\\b.*")
                    && !lowerCaseQuery.matches("(?s).*\\bscoped_" + blockedTableName + "\\b.*")) {
                    throw new BusinessException("사업장 관리자는 원본 테이블 대신 scoped_* 뷰만 조회할 수 있습니다.");
                }
            }
        }

        return normalized;
    }

    private String buildExecutableSql(Employee admin, String queryText) {
        if (!isWorkplaceScopedAdmin(admin)) {
            return queryText;
        }

        return """
            with scoped_workplace as (
                select *
                from workplaces
                where id = ?
            ),
            scoped_employees as (
                select *
                from employees
                where workplace_id = ?
                  and deleted = false
            ),
            scoped_attendance_records as (
                select ar.*
                from attendance_records ar
                join scoped_employees se on se.id = ar.employee_id
            ),
            user_query as (
            %s
            )
            select *
            from user_query
            """.formatted(queryText);
    }

    private InternalSqlQueryResultResponse runSqlQuery(String queryText, Employee admin, int rowLimit) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);
            try (PreparedStatement statement = connection.prepareStatement(queryText)) {
                if (isWorkplaceScopedAdmin(admin)) {
                    Long workplaceId = getAssignedWorkplaceId(admin);
                    statement.setLong(1, workplaceId);
                    statement.setLong(2, workplaceId);
                }
                statement.setMaxRows(rowLimit + 1);
                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    List<String> columns = new java.util.ArrayList<>(columnCount);
                    for (int index = 1; index <= columnCount; index++) {
                        columns.add(metaData.getColumnLabel(index));
                    }

                    List<List<String>> rows = new java.util.ArrayList<>();
                    boolean truncated = false;
                    while (resultSet.next()) {
                        if (rows.size() == rowLimit) {
                            truncated = true;
                            break;
                        }

                        List<String> row = new java.util.ArrayList<>(columnCount);
                        for (int index = 1; index <= columnCount; index++) {
                            Object value = resultSet.getObject(index);
                            row.add(value == null ? "" : String.valueOf(value));
                        }
                        rows.add(row);
                    }

                    return new InternalSqlQueryResultResponse(columns, rows, rowLimit, truncated);
                }
            }
        } catch (SQLException exception) {
            throw new BusinessException("SQL 실행에 실패했습니다. 구문과 테이블/컬럼명을 확인해 주세요.");
        }
    }

    private String trimExcelCellValue(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= EXCEL_CELL_TEXT_LIMIT ? value : value.substring(0, EXCEL_CELL_TEXT_LIMIT);
    }

    private String normalizeNoticeMessage(String noticeMessage) {
        if (!StringUtils.hasText(noticeMessage)) {
            return null;
        }
        return noticeMessage.trim();
    }

    private String normalizeMobileSkinKey(String mobileSkinKey) {
        if (!StringUtils.hasText(mobileSkinKey)) {
            return "classic";
        }

        return switch (mobileSkinKey.trim().toLowerCase()) {
            case "ocean" -> "ocean";
            case "sunset" -> "sunset";
            default -> "classic";
        };
    }

    private EmployeeRole parseRole(String rawRole) {
        try {
            EmployeeRole role = EmployeeRole.valueOf(rawRole.trim().toUpperCase());
            if (role == EmployeeRole.ADMIN) {
                throw new BusinessException("직원 초대에서는 ADMIN 역할을 생성할 수 없습니다.");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("지원하지 않는 역할입니다. EMPLOYEE 또는 WORKPLACE_ADMIN만 사용할 수 있습니다.");
        }
    }

    private EmployeeRole parseManageableRole(String rawRole) {
        try {
            return EmployeeRole.valueOf(rawRole.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("지원하지 않는 역할입니다.");
        }
    }

    private Workplace resolveWorkplace(Long companyId, Long workplaceId, EmployeeRole role) {
        if (workplaceId == null) {
            if (role == EmployeeRole.WORKPLACE_ADMIN) {
                throw new BusinessException("WORKPLACE_ADMIN은 사업장 지정이 필요합니다.");
            }
            return null;
        }

        Workplace workplace = workplaceRepository.findByIdAndCompanyId(workplaceId, companyId)
            .orElseThrow(() -> new BusinessException("해당 회사에 속한 사업장을 찾을 수 없습니다."));

        return workplace;
    }

    private Employee getEditableEmployee(String adminEmployeeCode, Long employeeId) {
        Employee admin = getEmployeeByCode(adminEmployeeCode);
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다."));

        if (!employee.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new BusinessException("같은 회사 직원만 관리할 수 있습니다.");
        }

        if (admin.getRole() == EmployeeRole.WORKPLACE_ADMIN) {
            Long assignedWorkplaceId = getAssignedWorkplaceId(admin);
            Long employeeWorkplaceId = employee.getWorkplace() == null ? null : employee.getWorkplace().getId();
            if (!java.util.Objects.equals(assignedWorkplaceId, employeeWorkplaceId)) {
                throw new BusinessException("해당 사업장 직원만 관리할 수 있습니다.");
            }
        }

        return employee;
    }

    private Workplace resolveManagedWorkplace(Employee admin, Long workplaceId) {
        Long requestedWorkplaceId = resolveRequestedWorkplaceId(admin, workplaceId);
        if (requestedWorkplaceId == null) {
            return null;
        }

        return workplaceRepository.findByIdAndCompanyId(requestedWorkplaceId, admin.getCompany().getId())
            .orElseThrow(() -> new BusinessException("해당 회사에 속한 사업장을 찾을 수 없습니다."));
    }

    private Workplace getManagedWorkplace(Employee admin, Long workplaceId) {
        Workplace workplace = workplaceRepository.findByIdAndCompanyId(workplaceId, admin.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("사업장을 찾을 수 없습니다."));

        if (isWorkplaceScopedAdmin(admin) && !java.util.Objects.equals(getAssignedWorkplaceId(admin), workplaceId)) {
            throw new BusinessException("담당 사업장만 수정할 수 있습니다.");
        }

        return workplace;
    }

    private void validateRoleAssignment(Employee admin, EmployeeRole targetRole, Long workplaceId) {
        if (targetRole == EmployeeRole.ADMIN) {
            throw new BusinessException("관리자 계정은 여기서 생성하거나 수정할 수 없습니다.");
        }

        if (targetRole == EmployeeRole.WORKPLACE_ADMIN && workplaceId == null) {
            throw new BusinessException("WORKPLACE_ADMIN은 사업장 지정이 필요합니다.");
        }

        if (admin.getRole() == EmployeeRole.WORKPLACE_ADMIN && targetRole != EmployeeRole.EMPLOYEE) {
            throw new BusinessException("사업장 관리자는 직원 계정만 관리할 수 있습니다.");
        }
    }

    private void validateDuplicateEmployeeCode(String employeeCode, Long employeeId) {
        String normalizedCode = employeeCode == null ? "" : employeeCode.trim();
        boolean exists = employeeId == null
            ? employeeRepository.existsByEmployeeCode(normalizedCode)
            : employeeRepository.existsByEmployeeCodeAndIdNot(normalizedCode, employeeId);

        if (exists) {
            throw new BusinessException("이미 사용 중인 직원 아이디입니다.");
        }
    }

    private LocalTime parseOptionalTime(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeException exception) {
            throw new BusinessException(label + " 형식이 올바르지 않습니다. HH:mm 형식으로 입력해 주세요.");
        }
    }

    private void applyPasswordChangePolicy(Employee employee, EmployeeRole role) {
        if (role == EmployeeRole.EMPLOYEE) {
            employee.markPasswordChangeRequired();
            return;
        }

        employee.markPasswordChanged();
    }

    private boolean matchesWorkplaceScope(Employee admin, Employee employee) {
        if (!isWorkplaceScopedAdmin(admin)) {
            return true;
        }

        return java.util.Objects.equals(getAssignedWorkplaceId(admin), employee.getWorkplace() == null ? null : employee.getWorkplace().getId());
    }

    private boolean matchesRequestedWorkplace(Employee employee, Long requestedWorkplaceId) {
        if (requestedWorkplaceId == null) {
            return true;
        }

        if (requestedWorkplaceId == 0L) {
            return employee.getWorkplace() == null;
        }

        Long employeeWorkplaceId = employee.getWorkplace() == null ? null : employee.getWorkplace().getId();
        return java.util.Objects.equals(employeeWorkplaceId, requestedWorkplaceId);
    }

    private Long resolveRequestedWorkplaceId(Employee admin, Long requestedWorkplaceId) {
        if (isWorkplaceScopedAdmin(admin)) {
            return getAssignedWorkplaceId(admin);
        }
        return requestedWorkplaceId;
    }

    private boolean isWorkplaceScopedAdmin(Employee employee) {
        return employee.getRole() == EmployeeRole.WORKPLACE_ADMIN;
    }

    private List<Workplace> getAccessibleWorkplaces(Employee admin) {
        if (isWorkplaceScopedAdmin(admin)) {
            Long workplaceId = getAssignedWorkplaceId(admin);
            if (workplaceId == null) {
                return List.of();
            }

            Workplace workplace = workplaceRepository.findByIdAndCompanyId(workplaceId, admin.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("사업장을 찾을 수 없습니다."));
            return List.of(workplace);
        }

        return workplaceRepository.findAllByCompanyIdOrderByNameAsc(admin.getCompany().getId());
    }

    private Long getAssignedWorkplaceId(Employee employee) {
        return employee.getWorkplace() == null ? null : employee.getWorkplace().getId();
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.toString();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.toLocalTime().toString();
    }

    private String toAttendanceState(AttendanceRecord record) {
        if (record == null) {
            return "ABSENT";
        }

        if (record.getCheckOutTime() != null) {
            return "CHECKED_OUT";
        }

        return record.isLate() ? "LATE" : "WORKING";
    }

    private boolean matchesDashboardFilter(String state, String filter) {
        String normalizedFilter = normalizeDashboardFilter(filter);
        if ("ALL".equals(normalizedFilter)) {
            return true;
        }
        if ("PRESENT".equals(normalizedFilter)) {
            return "WORKING".equals(state);
        }
        return normalizedFilter.equals(state);
    }

    private String normalizeDashboardFilter(String filter) {
        if (!StringUtils.hasText(filter)) {
            return "ALL";
        }

        String normalized = filter.trim().toUpperCase();
        return switch (normalized) {
            case "ALL", "PRESENT", "LATE", "ABSENT", "CHECKED_OUT" -> normalized;
            default -> "ALL";
        };
    }

    private String buildAttendanceNote(AttendanceRecord record) {
        if (record == null) {
            return "오늘 출근 기록 없음";
        }
        if (record.isLate()) {
            return "지각 출근";
        }
        if (record.getCheckOutTime() != null) {
            return "퇴근 완료";
        }
        return "근무 중";
    }

    private void validateUploadRow(int rowNumber,
                                   String employeeCode,
                                   String name,
                                   String roleValue,
                                   String password,
                                   String workplaceName,
                                   String workStartTime,
                                   String workEndTime,
                                   Set<String> existingCodes) {
        if (!StringUtils.hasText(employeeCode)) {
            throw new BusinessException(rowNumber + "행: 사번을 입력해 주세요.");
        }
        if (employeeCode.length() > 50) {
            throw new BusinessException(rowNumber + "행: 사번은 50자 이하여야 합니다.");
        }
        if (existingCodes.contains(employeeCode) || employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new BusinessException(rowNumber + "행: 이미 사용 중인 사번입니다. (" + employeeCode + ")");
        }
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(rowNumber + "행: 이름을 입력해 주세요.");
        }
        if (name.length() > 100) {
            throw new BusinessException(rowNumber + "행: 이름은 100자 이하여야 합니다.");
        }
        if (!StringUtils.hasText(roleValue)) {
            throw new BusinessException(rowNumber + "행: 권한을 입력해 주세요.");
        }
        if (!roleValue.equals("ADMIN") && !roleValue.equals("WORKPLACE_ADMIN") && !roleValue.equals("EMPLOYEE")) {
            throw new BusinessException(rowNumber + "행: 권한은 ADMIN, WORKPLACE_ADMIN 또는 EMPLOYEE만 사용할 수 있습니다.");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(rowNumber + "행: 비밀번호를 입력해 주세요.");
        }
        if (password.length() < 8) {
            throw new BusinessException(rowNumber + "행: 비밀번호는 8자 이상이어야 합니다.");
        }
        if (workplaceName != null && workplaceName.length() > 100) {
            throw new BusinessException(rowNumber + "행: 사업장명은 100자 이하여야 합니다.");
        }
        parseOptionalTime(workStartTime, rowNumber + "행 출근 기준 시간");
        parseOptionalTime(workEndTime, rowNumber + "행 퇴근 기준 시간");
    }

    private Workplace resolveUploadWorkplace(Employee admin,
                                             int rowNumber,
                                             String workplaceName,
                                             Map<String, Workplace> workplacesByName) {
        if (isWorkplaceScopedAdmin(admin)) {
            if (StringUtils.hasText(workplaceName)
                && !admin.getWorkplace().getName().equals(workplaceName.trim())) {
                throw new BusinessException(rowNumber + "행: 사업장 관리자 권한으로는 본인 사업장 직원만 등록할 수 있습니다.");
            }
            return admin.getWorkplace();
        }
        if (!StringUtils.hasText(workplaceName)) {
            return null;
        }

        Workplace workplace = workplacesByName.get(workplaceName.trim().toLowerCase());
        if (workplace == null) {
            throw new BusinessException(rowNumber + "행: 등록되지 않은 사업장입니다. (" + workplaceName + ")");
        }
        return workplace;
    }

    private void validateUploadRole(Employee admin, int rowNumber, String roleValue) {
        if (isWorkplaceScopedAdmin(admin) && !roleValue.equals("EMPLOYEE")) {
            throw new BusinessException(rowNumber + "행: 사업장 관리자 권한으로는 일반 직원만 일괄 등록할 수 있습니다.");
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < 7; index++) {
            if (!readCell(row, index).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row, int cellIndex) {
        Cell cell = row == null ? null : row.getCell(cellIndex);
        return cell == null ? "" : EXCEL_DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private InternalMonthlyAttendanceEmployeeDetailResponse buildMonthlyEmployeeDetailResponse(
        List<Employee> employees,
        Map<Long, List<AttendanceRecord>> recordsByEmployeeId,
        String selectedEmployeeCode
    ) {
        if (!StringUtils.hasText(selectedEmployeeCode)) {
            return null;
        }

        return employees.stream()
            .filter(employee -> employee.getEmployeeCode().equalsIgnoreCase(selectedEmployeeCode.trim()))
            .findFirst()
            .map(employee -> {
                List<AttendanceRecord> employeeRecords = recordsByEmployeeId.getOrDefault(employee.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(AttendanceRecord::getAttendanceDate).reversed()
                        .thenComparing(AttendanceRecord::getCheckInTime, Comparator.reverseOrder()))
                    .toList();

                int lateDays = (int) employeeRecords.stream().filter(AttendanceRecord::isLate).count();
                int checkedOutDays = (int) employeeRecords.stream().filter(record -> record.getCheckOutTime() != null).count();

                List<InternalMonthlyAttendanceRecordRowResponse> records = employeeRecords.stream()
                    .map(record -> new InternalMonthlyAttendanceRecordRowResponse(
                        record.getAttendanceDate().format(DATE_FORMATTER),
                        record.getEmployee().getEmployeeCode(),
                        record.getEmployee().getName(),
                        record.getEmployee().getWorkplace() == null ? "본사" : record.getEmployee().getWorkplace().getName(),
                        record.getEmployee().getRole().name(),
                        toAttendanceState(record),
                        formatDateTime(record.getCheckInTime()),
                        formatDateTime(record.getCheckOutTime()),
                        buildAttendanceNote(record)
                    ))
                    .toList();

                return new InternalMonthlyAttendanceEmployeeDetailResponse(
                    employee.getEmployeeCode(),
                    employee.getName(),
                    employee.getWorkplace() == null ? "본사" : employee.getWorkplace().getName(),
                    employee.getRole().name(),
                    employeeRecords.size(),
                    lateDays,
                    checkedOutDays,
                    records
                );
            })
            .orElse(null);
    }

    private AttendanceRecord pickLatestAttendanceRecord(AttendanceRecord left, AttendanceRecord right) {
        return Comparator
            .comparing(AttendanceRecord::getCheckInTime)
            .thenComparing(AttendanceRecord::getId)
            .compare(left, right) >= 0 ? left : right;
    }

    private String buildInviteUrl(String token) {
        if (!StringUtils.hasText(inviteProperties.getBaseUrl())) {
            return "/invite?token=" + token;
        }

        String baseUrl = inviteProperties.getBaseUrl().trim();
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + token;
    }
}
