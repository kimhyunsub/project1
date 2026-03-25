package com.attendance.backend.service;

import com.attendance.backend.domain.entity.AttendanceRecord;
import com.attendance.backend.domain.entity.Company;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.repository.AttendanceRecordRepository;
import com.attendance.backend.domain.repository.CompanyRepository;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.dto.admin.CompanySettingResponse;
import com.attendance.backend.dto.admin.EmployeeSummaryResponse;
import com.attendance.backend.dto.admin.TodayAttendanceOverviewResponse;
import com.attendance.backend.dto.admin.UpdateAttendanceRadiusRequest;
import com.attendance.backend.dto.admin.UpdateCompanyLocationRequest;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.exception.ResourceNotFoundException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingRepository companySettingRepository;
    private final AttendanceExcelService attendanceExcelService;

    public AdminService(
        EmployeeRepository employeeRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        CompanyRepository companyRepository,
        CompanySettingRepository companySettingRepository,
        AttendanceExcelService attendanceExcelService
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.companyRepository = companyRepository;
        this.companySettingRepository = companySettingRepository;
        this.attendanceExcelService = attendanceExcelService;
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

    public List<TodayAttendanceOverviewResponse> getTodayAttendance(Long adminEmployeeId) {
        Employee admin = getEmployee(adminEmployeeId);
        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);

        List<Employee> employees = employeeRepository.findAllByCompanyIdAndDeletedFalseOrderByNameAsc(admin.getCompany().getId());
        Map<Long, AttendanceRecord> recordsByEmployeeId =
            attendanceRecordRepository.findAllByEmployeeCompanyIdAndAttendanceDate(admin.getCompany().getId(), today)
                .stream()
                .collect(Collectors.toMap(record -> record.getEmployee().getId(), Function.identity()));

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
            setting.isEnforceSingleDeviceLogin(),
            "출근 반경이 수정되었습니다."
        );
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private CompanySetting getCompanySetting(Company company) {
        return companySettingRepository.findByCompany(company)
            .orElseThrow(() -> new ResourceNotFoundException("회사 설정을 찾을 수 없습니다."));
    }
}
