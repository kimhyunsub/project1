package com.attendance.backend.service;

import com.attendance.backend.domain.entity.AttendanceRecord;
import com.attendance.backend.domain.entity.AttendanceActionType;
import com.attendance.backend.domain.entity.AttendanceStatus;
import com.attendance.backend.domain.entity.Company;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.Workplace;
import com.attendance.backend.domain.repository.AttendanceRecordRepository;
import com.attendance.backend.domain.repository.CompanyRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private static final double MAX_LOCATION_ACCURACY_METERS = 100;
    private static final Duration MAX_LOCATION_AGE = Duration.ofMinutes(2);
    private static final Duration MAX_FUTURE_SKEW = Duration.ofSeconds(30);
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingRepository companySettingRepository;
    private final AttendanceActionLogService attendanceActionLogService;

    public AttendanceService(
        EmployeeRepository employeeRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        CompanyRepository companyRepository,
        CompanySettingRepository companySettingRepository,
        AttendanceActionLogService attendanceActionLogService
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.companyRepository = companyRepository;
        this.companySettingRepository = companySettingRepository;
        this.attendanceActionLogService = attendanceActionLogService;
    }

    @Transactional
    public CheckInResponse checkIn(Long employeeId, CheckInRequest request) {
        Employee employee = getEmployee(employeeId);
        LocalDate today = currentDateInSeoul();
        Double distanceMeters = null;

        try {
            attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .ifPresent(record -> {
                    throw new BusinessException("오늘은 이미 출근 처리되었습니다.");
                });

            Company company = employee.getCompany();
            CompanySetting companySetting = getCompanySetting(company);

            validateMockLocation(request.getMockLocation(), "출근");

            distanceMeters = validateLocationProof(
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracyMeters(),
                request.getCapturedAt(),
                employee,
                companySetting,
                "출근"
            );

            LocalDateTime checkInTime = currentDateTimeInSeoul();
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

            String message = savedRecord.isLate() ? "지각으로 출근 처리되었습니다." : "정상 출근 처리되었습니다.";
            logAction(employee, AttendanceActionType.CHECK_IN, today, request, distanceMeters, true, message);

            return new CheckInResponse(
                savedRecord.getCheckInTime(),
                savedRecord.isLate(),
                message
            );
        } catch (RuntimeException exception) {
            logAction(employee, AttendanceActionType.CHECK_IN, today, request, distanceMeters, false, exception.getMessage());
            throw exception;
        }
    }

    @Transactional
    public CheckOutResponse checkOut(Long employeeId, CheckOutRequest request) {
        Employee employee = getEmployee(employeeId);
        LocalDate today = currentDateInSeoul();
        Double distanceMeters = null;

        try {
            AttendanceRecord record = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new BusinessException("오늘 출근 기록이 없어 퇴근 처리할 수 없습니다."));
            boolean alreadyCheckedOut = record.getCheckOutTime() != null;

            validateMockLocation(request.getMockLocation(), "퇴근");

            record.checkOut(currentDateTimeInSeoul(), request.getLatitude(), request.getLongitude());
            String message = alreadyCheckedOut
                ? "퇴근 시간이 최신 시각으로 업데이트되었습니다."
                : "퇴근이 정상 처리되었습니다.";
            logAction(employee, AttendanceActionType.CHECK_OUT, today, request, distanceMeters, true, message);

            return new CheckOutResponse(
                record.getId(),
                record.getAttendanceDate(),
                record.getCheckInTime(),
                record.getCheckOutTime(),
                record.getStatus(),
                message
            );
        } catch (RuntimeException exception) {
            logAction(employee, AttendanceActionType.CHECK_OUT, today, request, distanceMeters, false, exception.getMessage());
            throw exception;
        }
    }

    public TodayAttendanceStatusResponse getTodayStatus(Long employeeId) {
        Employee employee = getEmployee(employeeId);
        LocalDate today = currentDateInSeoul();
        return attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
            .map(record -> new TodayAttendanceStatusResponse(
                true,
                record.getAttendanceDate(),
                record.getCheckInTime(),
                record.getCheckOutTime(),
                record.getStatus(),
                employee.getCompany().getName(),
                employee.getWorkplace() == null ? null : employee.getWorkplace().getName()
            ))
            .orElseGet(() -> new TodayAttendanceStatusResponse(
                false,
                today,
                null,
                null,
                null,
                employee.getCompany().getName(),
                employee.getWorkplace() == null ? null : employee.getWorkplace().getName()
            ));
    }

    public CompanySettingResponse getCompanySetting(Long employeeId) {
        Employee employee = getEmployee(employeeId);
        Company company = employee.getCompany();
        CompanySetting setting = getCompanySetting(company);
        Workplace workplace = employee.getWorkplace();

        return new CompanySettingResponse(
            company.getId(),
            company.getName(),
            workplace == null ? null : workplace.getId(),
            workplace == null ? null : workplace.getName(),
            workplace == null ? company.getLatitude() : workplace.getLatitude(),
            workplace == null ? company.getLongitude() : workplace.getLongitude(),
            workplace == null ? setting.getAllowedRadiusMeters() : workplace.getAllowedRadiusMeters(),
            setting.getLateAfterTime(),
            workplace == null || workplace.getNoticeMessage() == null || workplace.getNoticeMessage().isBlank()
                ? setting.getNoticeMessage()
                : workplace.getNoticeMessage(),
            setting.getMobileSkinKey(),
            setting.isEnforceSingleDeviceLogin(),
            "회사 설정 조회가 완료되었습니다."
        );
    }

    public CompanySettingResponse getPublicCompanySetting() {
        Company company = companyRepository.findFirstByOrderByIdAsc()
            .orElseThrow(() -> new ResourceNotFoundException("회사를 찾을 수 없습니다."));
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

    private LocalDate currentDateInSeoul() {
        return LocalDate.now(SEOUL_ZONE_ID);
    }

    private LocalDateTime currentDateTimeInSeoul() {
        return LocalDateTime.now(SEOUL_ZONE_ID);
    }

    private boolean isLate(LocalTime checkInTime, LocalTime lateAfterTime) {
        return checkInTime.isAfter(lateAfterTime);
    }

    private void validateMockLocation(Boolean mockLocation, String actionLabel) {
        if (Boolean.TRUE.equals(mockLocation)) {
            throw new BusinessException("위치 변조가 감지되어 " + actionLabel + " 처리할 수 없습니다.");
        }
    }

    private double validateLocationProof(
        double latitude,
        double longitude,
        double accuracyMeters,
        Instant capturedAt,
        Employee employee,
        CompanySetting companySetting,
        String actionLabel
    ) {
        Instant now = Instant.now();
        if (capturedAt.isBefore(now.minus(MAX_LOCATION_AGE))) {
            throw new BusinessException(actionLabel + "에 사용된 위치 정보가 너무 오래되었습니다. 위치를 새로고침한 뒤 다시 시도해 주세요.");
        }

        if (capturedAt.isAfter(now.plus(MAX_FUTURE_SKEW))) {
            throw new BusinessException(actionLabel + " 위치 측정 시간이 올바르지 않습니다. 단말 시간을 확인해 주세요.");
        }

        if (accuracyMeters > MAX_LOCATION_ACCURACY_METERS) {
            throw new BusinessException(
                actionLabel + " 위치 정확도가 너무 낮습니다. 현재 정확도: " + Math.round(accuracyMeters)
                    + "m, 요구 정확도: " + (int) MAX_LOCATION_ACCURACY_METERS + "m 이하"
            );
        }

        Company company = employee.getCompany();
        Workplace workplace = employee.getWorkplace();
        double targetLatitude = workplace == null ? company.getLatitude() : workplace.getLatitude();
        double targetLongitude = workplace == null ? company.getLongitude() : workplace.getLongitude();
        int allowedRadiusMeters = companySetting.getAllowedRadiusMeters();
        String locationLabel = workplace == null ? "회사" : workplace.getName();

        if (workplace != null) {
            allowedRadiusMeters = workplace.getAllowedRadiusMeters();
        }

        double distanceMeters = DistanceCalculator.calculateMeters(
            targetLatitude,
            targetLongitude,
            latitude,
            longitude
        );

        if (distanceMeters > allowedRadiusMeters) {
            throw new BusinessException(
                locationLabel + " 반경 " + allowedRadiusMeters + "m 이내에서만 " + actionLabel
                    + "할 수 있습니다. 현재 거리: " + Math.round(distanceMeters) + "m"
            );
        }

        return distanceMeters;
    }

    private void logAction(
        Employee employee,
        AttendanceActionType actionType,
        LocalDate attendanceDate,
        CheckInRequest request,
        Double distanceMeters,
        boolean success,
        String message
    ) {
        attendanceActionLogService.logAttempt(
            employee,
            actionType,
            attendanceDate,
            request.getLatitude(),
            request.getLongitude(),
            request.getAccuracyMeters(),
            request.getCapturedAt(),
            distanceMeters,
            success,
            message
        );
    }

    private void logAction(
        Employee employee,
        AttendanceActionType actionType,
        LocalDate attendanceDate,
        CheckOutRequest request,
        Double distanceMeters,
        boolean success,
        String message
    ) {
        attendanceActionLogService.logAttempt(
            employee,
            actionType,
            attendanceDate,
            request.getLatitude(),
            request.getLongitude(),
            request.getAccuracyMeters(),
            request.getCapturedAt(),
            distanceMeters,
            success,
            message
        );
    }
}
