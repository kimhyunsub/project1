package com.attendance.backend.service;

import com.attendance.backend.domain.entity.AttendanceRecord;
import com.attendance.backend.domain.entity.AttendanceStatus;
import com.attendance.backend.domain.entity.Company;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.repository.AttendanceRecordRepository;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.dto.attendance.CheckInRequest;
import com.attendance.backend.dto.attendance.CheckInResponse;
import com.attendance.backend.dto.attendance.CheckOutRequest;
import com.attendance.backend.dto.attendance.CheckOutResponse;
import com.attendance.backend.dto.attendance.TodayAttendanceStatusResponse;
import com.attendance.backend.dto.admin.CompanySettingResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.exception.ResourceNotFoundException;
import com.attendance.backend.util.DistanceCalculator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CompanySettingRepository companySettingRepository;

    public AttendanceService(
        EmployeeRepository employeeRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        CompanySettingRepository companySettingRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.companySettingRepository = companySettingRepository;
    }

    @Transactional
    public CheckInResponse checkIn(Long employeeId, CheckInRequest request) {
        Employee employee = getEmployee(employeeId);
        LocalDate today = LocalDate.now();

        attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
            .ifPresent(record -> {
                throw new BusinessException("오늘은 이미 출근 처리되었습니다.");
            });

        Company company = employee.getCompany();
        CompanySetting companySetting = getCompanySetting(company);

        double distanceMeters = DistanceCalculator.calculateMeters(
            company.getLatitude(),
            company.getLongitude(),
            request.getLatitude(),
            request.getLongitude()
        );

        if (distanceMeters > companySetting.getAllowedRadiusMeters()) {
            throw new BusinessException(
                "회사 반경 " + companySetting.getAllowedRadiusMeters() + "m 이내에서만 출근할 수 있습니다. 현재 거리: "
                    + Math.round(distanceMeters) + "m"
            );
        }

        LocalDateTime checkInTime = LocalDateTime.now();
        LocalTime lateReferenceTime = employee.getWorkStartTime() == null
            ? companySetting.getLateAfterTime()
            : employee.getWorkStartTime();
        AttendanceRecord savedRecord = attendanceRecordRepository.save(
            new AttendanceRecord(
                employee,
                today,
                checkInTime,
                request.getLatitude(),
                request.getLongitude(),
                isLate(checkInTime.toLocalTime(), lateReferenceTime),
                AttendanceStatus.CHECKED_IN
            )
        );

        return new CheckInResponse(
            savedRecord.getCheckInTime(),
            savedRecord.isLate(),
            savedRecord.isLate() ? "지각으로 출근 처리되었습니다." : "정상 출근 처리되었습니다."
        );
    }

    @Transactional
    public CheckOutResponse checkOut(Long employeeId, CheckOutRequest request) {
        AttendanceRecord record = attendanceRecordRepository
            .findByEmployeeIdAndAttendanceDate(employeeId, LocalDate.now())
            .orElseThrow(() -> new BusinessException("오늘 출근 기록이 없어 퇴근 처리할 수 없습니다."));

        if (record.getCheckOutTime() != null) {
            throw new BusinessException("이미 퇴근 처리되었습니다.");
        }

        record.checkOut(LocalDateTime.now(), request.getLatitude(), request.getLongitude());

        return new CheckOutResponse(
            record.getId(),
            record.getAttendanceDate(),
            record.getCheckInTime(),
            record.getCheckOutTime(),
            record.getStatus(),
            "퇴근이 정상 처리되었습니다."
        );
    }

    public TodayAttendanceStatusResponse getTodayStatus(Long employeeId) {
        Employee employee = getEmployee(employeeId);
        return attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(employeeId, LocalDate.now())
            .map(record -> new TodayAttendanceStatusResponse(
                true,
                record.getAttendanceDate(),
                record.getCheckInTime(),
                record.getCheckOutTime(),
                record.getStatus(),
                employee.getCompany().getName()
            ))
            .orElseGet(() -> new TodayAttendanceStatusResponse(
                false,
                LocalDate.now(),
                null,
                null,
                null,
                employee.getCompany().getName()
            ));
    }

    public CompanySettingResponse getCompanySetting(Long employeeId) {
        Employee employee = getEmployee(employeeId);
        Company company = employee.getCompany();
        CompanySetting setting = getCompanySetting(company);

        return new CompanySettingResponse(
            company.getId(),
            company.getName(),
            company.getLatitude(),
            company.getLongitude(),
            setting.getAllowedRadiusMeters(),
            setting.getLateAfterTime(),
            "회사 설정 조회가 완료되었습니다."
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

    private boolean isLate(LocalTime checkInTime, LocalTime lateAfterTime) {
        return checkInTime.isAfter(lateAfterTime);
    }
}
