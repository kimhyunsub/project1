package com.attendance.backend.service;

import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.EmployeeRole;
import com.attendance.backend.domain.entity.HalfDayType;
import com.attendance.backend.domain.entity.SpecialLeaveOccasionType;
import com.attendance.backend.domain.entity.WorkRequest;
import com.attendance.backend.domain.entity.WorkRequestStatus;
import com.attendance.backend.domain.entity.WorkRequestType;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.domain.repository.WorkRequestRepository;
import com.attendance.backend.dto.attendance.CreateWorkRequestRequest;
import com.attendance.backend.dto.attendance.WorkRequestActionResponse;
import com.attendance.backend.dto.attendance.WorkRequestCreateResponse;
import com.attendance.backend.dto.attendance.WorkRequestListResponse;
import com.attendance.backend.dto.attendance.WorkRequestResponse;
import com.attendance.backend.dto.internal.InternalWorkRequestCreateRequest;
import com.attendance.backend.dto.internal.InternalWorkRequestListResponse;
import com.attendance.backend.dto.internal.InternalWorkRequestResponse;
import com.attendance.backend.dto.internal.InternalWorkRequestUploadResponse;
import com.attendance.backend.exception.BusinessException;
import com.attendance.backend.exception.ResourceNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class WorkRequestService {

    private static final Logger log = LoggerFactory.getLogger(WorkRequestService.class);
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final List<WorkRequestStatus> ACTIVE_DUPLICATE_STATUSES =
        List.of(WorkRequestStatus.PENDING, WorkRequestStatus.APPROVED, WorkRequestStatus.CANCEL_REQUESTED);
    private static final DataFormatter EXCEL_DATA_FORMATTER = new DataFormatter();
    private static final List<DateTimeFormatter> UPLOAD_DATE_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("M/d/yy"),
        DateTimeFormatter.ofPattern("M/d/yyyy")
    );

    private final EmployeeRepository employeeRepository;
    private final CompanySettingRepository companySettingRepository;
    private final WorkRequestRepository workRequestRepository;

    public WorkRequestService(
        EmployeeRepository employeeRepository,
        CompanySettingRepository companySettingRepository,
        WorkRequestRepository workRequestRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.companySettingRepository = companySettingRepository;
        this.workRequestRepository = workRequestRepository;
    }

    @Transactional
    public WorkRequestCreateResponse createRequest(Long employeeId, CreateWorkRequestRequest request) {
        Employee employee = getEmployee(employeeId);
        CompanySetting setting = getCompanySetting(employee);
        assertWorkRequestEnabled(employee, setting);
        WorkRequestType requestType = parseRequestType(request.getRequestType());
        LocalDate requestDate = parseRequestDate(request.getRequestDate());
        HalfDayType halfDayType = parseHalfDayType(requestType, request.getHalfDayType());
        SpecialLeaveOccasionType occasionType = parseOccasionType(requestType, request.getOccasionType());
        Integer earlyLeaveMinutes = normalizeEarlyLeaveMinutes(requestType, request.getEarlyLeaveMinutes());
        String reason = normalizeReason(request.getReason());

        validateRequest(employee, requestType, requestDate, earlyLeaveMinutes, false);

        WorkRequestStatus initialStatus = isWorkRequestApprovalRequired(employee, setting)
            ? WorkRequestStatus.PENDING
            : WorkRequestStatus.APPROVED;

        WorkRequest saved = workRequestRepository.save(
            new WorkRequest(
                employee,
                employee.getCompany(),
                requestType,
                initialStatus,
                halfDayType,
                occasionType,
                requestDate,
                earlyLeaveMinutes,
                reason
            )
        );

        if (initialStatus == WorkRequestStatus.APPROVED) {
            saved.approve(employee, "승인 절차 없이 즉시 확정되었습니다.");
        }

        return new WorkRequestCreateResponse(
            toResponse(saved),
            initialStatus == WorkRequestStatus.PENDING
                ? "휴가 신청이 등록되었습니다. 관리자 승인 후 반영됩니다."
                : "휴가 신청이 즉시 반영되었습니다."
        );
    }

    public WorkRequestListResponse getRequests(Long employeeId) {
        Employee employee = getEmployee(employeeId);
        CompanySetting setting = getCompanySetting(employee);
        return new WorkRequestListResponse(
            resolveApprovalRequiredForList(employee, setting),
            isWorkRequestEnabled(employee, setting),
            loadRequestResponses(employeeId)
        );
    }

    @Transactional
    public WorkRequestActionResponse cancelRequest(Long employeeId, Long requestId) {
        WorkRequest request = workRequestRepository.findByIdAndEmployeeId(requestId, employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("신청 내역을 찾을 수 없습니다."));
        assertWorkRequestEnabled(request.getEmployee(), getCompanySetting(request.getEmployee()));

        if (request.isPending()) {
            request.cancel();
            return new WorkRequestActionResponse(toResponse(request), "신청이 취소되었습니다.");
        }
        if (request.isApproved()) {
            request.requestCancellation();
            return new WorkRequestActionResponse(toResponse(request), "취소 요청이 등록되었습니다. 관리자 승인 후 취소됩니다.");
        }

        throw new BusinessException("승인 대기 또는 승인된 신청만 취소할 수 있습니다.");
    }

    public InternalWorkRequestListResponse getRequestsForAdmin(String adminEmployeeCode) {
        Employee admin = getAdmin(adminEmployeeCode);
        CompanySetting setting = getCompanySetting(admin);
        List<WorkRequest> requests = isWorkplaceScopedAdmin(admin)
            ? workRequestRepository.findAllByCompanyIdAndEmployeeWorkplaceIdOrderByStatusAscRequestDateDescCreatedAtDesc(
                admin.getCompany().getId(),
                getRequiredWorkplaceId(admin)
            )
            : workRequestRepository.findAllByCompanyIdOrderByStatusAscRequestDateDescCreatedAtDesc(admin.getCompany().getId());

        return new InternalWorkRequestListResponse(
            isWorkRequestApprovalRequired(admin, setting),
            isWorkplaceScopedAdmin(admin),
            requests.stream()
                .filter(request -> request.getStatus() != WorkRequestStatus.CANCELED)
                .map(this::toInternalResponse)
                .toList()
        );
    }

    @Transactional
    public InternalWorkRequestResponse approveRequest(String adminEmployeeCode, Long requestId, String reviewNote) {
        Employee admin = getAdmin(adminEmployeeCode);
        WorkRequest request = getManageableRequest(admin, requestId);
        if (request.getStatus() != WorkRequestStatus.PENDING && request.getStatus() != WorkRequestStatus.REJECTED) {
            throw new BusinessException("승인 대기 또는 반려된 신청만 승인할 수 있습니다.");
        }
        request.approve(admin, normalizeReason(reviewNote));
        return toInternalResponse(request);
    }

    @Transactional
    public InternalWorkRequestResponse createRequestForAdmin(String adminEmployeeCode, InternalWorkRequestCreateRequest request) {
        Employee admin = getAdmin(adminEmployeeCode);
        Employee employee = getManageableEmployee(admin, request.getEmployeeCode());
        WorkRequest workRequest = createApprovedAdminRequest(
            admin,
            employee,
            request.getRequestType(),
            request.getRequestDate(),
            request.getHalfDayType(),
            request.getOccasionType(),
            request.getEarlyLeaveMinutes(),
            request.getReason()
        );
        return toInternalResponse(workRequest);
    }

    @Transactional
    public InternalWorkRequestUploadResponse uploadRequestsForAdmin(String adminEmployeeCode, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("업로드할 엑셀 파일을 선택해 주세요.");
        }

        Employee admin = getAdmin(adminEmployeeCode);
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

                try {
                    String employeeCode = readCell(row, 0);
                    String requestDate = readDateCell(row, 1);
                    String requestType = readCell(row, 2);
                    String halfDayType = readCell(row, 3);
                    Integer earlyLeaveMinutes = parseOptionalInteger(readCell(row, 4), rowIndex + 1 + "행 유연근무분");
                    String reason = readCell(row, 5);
                    String occasionType = readCell(row, 6);
                    Employee employee = getManageableEmployee(admin, employeeCode);

                    createApprovedAdminRequest(
                        admin,
                        employee,
                        requestType,
                        requestDate,
                        halfDayType,
                        occasionType,
                        earlyLeaveMinutes,
                        reason
                    );
                    successCount++;
                } catch (BusinessException exception) {
                    failureMessages.add((rowIndex + 1) + "행: " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new BusinessException("엑셀 파일을 읽는 중 오류가 발생했습니다.");
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("지원하지 않는 엑셀 파일 형식입니다. .xlsx 파일을 사용해 주세요.");
        }

        return new InternalWorkRequestUploadResponse(successCount, failureMessages.size(), failureMessages);
    }

    @Transactional
    public InternalWorkRequestResponse rejectRequest(String adminEmployeeCode, Long requestId, String reviewNote) {
        Employee admin = getAdmin(adminEmployeeCode);
        WorkRequest request = getManageableRequest(admin, requestId);
        if (!request.isPending()) {
            throw new BusinessException("승인 대기 중인 신청만 반려할 수 있습니다.");
        }
        request.reject(admin, normalizeReason(reviewNote));
        return toInternalResponse(request);
    }

    @Transactional
    public InternalWorkRequestResponse cancelRequestForAdmin(String adminEmployeeCode, Long requestId, String reviewNote) {
        Employee admin = getAdmin(adminEmployeeCode);
        WorkRequest request = getManageableRequest(admin, requestId);
        if (request.getStatus() != WorkRequestStatus.APPROVED
            && request.getStatus() != WorkRequestStatus.PENDING
            && request.getStatus() != WorkRequestStatus.REJECTED
            && request.getStatus() != WorkRequestStatus.CANCEL_REQUESTED) {
            throw new BusinessException("승인 대기, 승인, 반려 또는 취소 요청된 신청만 취소할 수 있습니다.");
        }
        request.cancel(admin, normalizeReason(reviewNote));
        return toInternalResponse(request);
    }

    private WorkRequest createApprovedAdminRequest(
        Employee admin,
        Employee employee,
        String rawRequestType,
        String rawRequestDate,
        String rawHalfDayType,
        String rawOccasionType,
        Integer rawEarlyLeaveMinutes,
        String rawReason
    ) {
        WorkRequestType requestType = parseRequestType(rawRequestType);
        LocalDate requestDate = parseRequestDate(rawRequestDate);
        HalfDayType halfDayType = parseHalfDayType(requestType, rawHalfDayType);
        SpecialLeaveOccasionType occasionType = parseOccasionType(requestType, rawOccasionType);
        Integer earlyLeaveMinutes = normalizeEarlyLeaveMinutes(requestType, rawEarlyLeaveMinutes);
        String reason = normalizeReason(rawReason);

        validateRequest(employee, requestType, requestDate, earlyLeaveMinutes, true);

        WorkRequest saved = workRequestRepository.save(
            new WorkRequest(
                employee,
                employee.getCompany(),
                requestType,
                WorkRequestStatus.APPROVED,
                halfDayType,
                occasionType,
                requestDate,
                earlyLeaveMinutes,
                reason
            )
        );
        saved.approve(admin, "관리자가 직접 등록했습니다.");
        return saved;
    }

    private void validateRequest(
        Employee employee,
        WorkRequestType requestType,
        LocalDate requestDate,
        Integer earlyLeaveMinutes,
        boolean allowHistoricalDate
    ) {
        if (!allowHistoricalDate && requestDate.isBefore(LocalDate.now(SEOUL_ZONE_ID).minusMonths(1))) {
            throw new BusinessException("한 달 이전 날짜로는 신청할 수 없습니다.");
        }
        if (workRequestRepository.existsByEmployeeIdAndRequestDateAndRequestTypeInAndStatusIn(
            employee.getId(),
            requestDate,
            conflictingRequestTypes(requestType),
            ACTIVE_DUPLICATE_STATUSES
        )) {
            throw new BusinessException("해당 날짜에는 함께 사용할 수 없는 처리 중이거나 승인된 신청이 있습니다.");
        }
        if (requestType == WorkRequestType.EARLY_LEAVE && employee.getWorkEndTime() == null && earlyLeaveMinutes == null) {
            throw new BusinessException("유연근무 시간 정보를 확인할 수 없습니다.");
        }
    }

    private List<WorkRequestType> conflictingRequestTypes(WorkRequestType requestType) {
        return switch (requestType) {
            case VACATION, SPECIAL_LEAVE -> List.of(
                WorkRequestType.VACATION,
                WorkRequestType.HALF_DAY,
                WorkRequestType.EARLY_LEAVE,
                WorkRequestType.SPECIAL_LEAVE
            );
            case HALF_DAY -> List.of(WorkRequestType.VACATION, WorkRequestType.HALF_DAY, WorkRequestType.SPECIAL_LEAVE);
            case EARLY_LEAVE -> List.of(WorkRequestType.VACATION, WorkRequestType.EARLY_LEAVE, WorkRequestType.SPECIAL_LEAVE);
        };
    }

    private WorkRequest getManageableRequest(Employee admin, Long requestId) {
        WorkRequest request = workRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("신청 내역을 찾을 수 없습니다."));
        if (!request.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new BusinessException("다른 회사의 신청은 관리할 수 없습니다.");
        }
        if (isWorkplaceScopedAdmin(admin)) {
            Long workplaceId = getRequiredWorkplaceId(admin);
            if (request.getEmployee().getWorkplace() == null || !workplaceId.equals(request.getEmployee().getWorkplace().getId())) {
                throw new BusinessException("담당 사업장 직원의 신청만 관리할 수 있습니다.");
            }
        }
        return request;
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private Employee getAdmin(String adminEmployeeCode) {
        Employee admin = employeeRepository.findByEmployeeCode(adminEmployeeCode)
            .orElseThrow(() -> new ResourceNotFoundException("관리자를 찾을 수 없습니다."));
        if (admin.getRole() != EmployeeRole.ADMIN && admin.getRole() != EmployeeRole.WORKPLACE_ADMIN) {
            throw new BusinessException("관리자 권한이 필요합니다.");
        }
        return admin;
    }

    private Employee getManageableEmployee(Employee admin, String employeeCode) {
        if (!StringUtils.hasText(employeeCode)) {
            throw new BusinessException("사번을 입력해 주세요.");
        }

        Employee employee = employeeRepository.findByEmployeeCode(employeeCode.trim())
            .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다."));
        if (!employee.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new BusinessException("다른 회사 직원은 등록할 수 없습니다.");
        }
        if (employee.isDeleted()) {
            throw new BusinessException("삭제된 직원은 등록할 수 없습니다.");
        }
        if (isWorkplaceScopedAdmin(admin)) {
            Long workplaceId = getRequiredWorkplaceId(admin);
            if (employee.getWorkplace() == null || !workplaceId.equals(employee.getWorkplace().getId())) {
                throw new BusinessException("담당 사업장 직원만 등록할 수 있습니다.");
            }
        }
        return employee;
    }

    private CompanySetting getCompanySetting(Employee employee) {
        return companySettingRepository.findByCompany(employee.getCompany())
            .orElseThrow(() -> new ResourceNotFoundException("회사 설정을 찾을 수 없습니다."));
    }

    private boolean resolveApprovalRequiredForList(Employee employee, CompanySetting setting) {
        try {
            return isWorkRequestApprovalRequired(employee, setting);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to resolve work request approval setting. employeeId={}",
                employee.getId(),
                exception
            );
            return true;
        }
    }

    private List<WorkRequestResponse> loadRequestResponses(Long employeeId) {
        try {
            return workRequestRepository.findAllByEmployeeIdOrderByRequestDateDescCreatedAtDesc(employeeId)
                .stream()
                .filter(request -> request.getStatus() != WorkRequestStatus.CANCELED)
                .map(this::toResponse)
                .toList();
        } catch (RuntimeException exception) {
            log.warn("Failed to load work request list. employeeId={}", employeeId, exception);
            return List.of();
        }
    }

    private boolean isWorkRequestApprovalRequired(Employee employee, CompanySetting setting) {
        return employee.getWorkplace() == null
            ? setting.isWorkRequestApprovalRequired()
            : employee.getWorkplace().isWorkRequestApprovalRequired();
    }

    private boolean isWorkRequestEnabled(Employee employee, CompanySetting setting) {
        return employee.getWorkplace() == null
            ? setting.isWorkRequestEnabled()
            : employee.getWorkplace().isWorkRequestEnabled();
    }

    private void assertWorkRequestEnabled(Employee employee, CompanySetting setting) {
        if (!isWorkRequestEnabled(employee, setting)) {
            throw new BusinessException("휴가신청 기능이 비활성화되어 있습니다.");
        }
    }

    private boolean isWorkplaceScopedAdmin(Employee admin) {
        return admin.getRole() == EmployeeRole.WORKPLACE_ADMIN;
    }

    private Long getRequiredWorkplaceId(Employee admin) {
        if (admin.getWorkplace() == null) {
            throw new BusinessException("사업장 관리자의 소속 사업장을 찾을 수 없습니다.");
        }
        return admin.getWorkplace().getId();
    }

    private WorkRequestType parseRequestType(String rawValue) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        String normalized = trimmed.toUpperCase();
        if ("휴가".equals(trimmed) || "연차".equals(trimmed)) {
            return WorkRequestType.VACATION;
        }
        if ("반차".equals(trimmed)) {
            return WorkRequestType.HALF_DAY;
        }
        if ("유연근무".equals(trimmed) || "조기퇴근".equals(trimmed)) {
            return WorkRequestType.EARLY_LEAVE;
        }
        if ("경조사".equals(trimmed) || "특별휴가".equals(trimmed) || "특별 휴가".equals(trimmed)) {
            return WorkRequestType.SPECIAL_LEAVE;
        }
        try {
            return WorkRequestType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("지원하지 않는 신청 유형입니다.");
        }
    }

    private SpecialLeaveOccasionType parseOccasionType(WorkRequestType requestType, String rawValue) {
        if (requestType != WorkRequestType.SPECIAL_LEAVE) {
            return null;
        }
        String trimmed = rawValue == null ? "" : rawValue.trim();
        String normalized = trimmed.toUpperCase();
        if ("본인결혼".equals(trimmed) || "본인 결혼".equals(trimmed) || "결혼".equals(trimmed)) {
            return SpecialLeaveOccasionType.SELF_MARRIAGE;
        }
        if ("자녀결혼".equals(trimmed) || "자녀 결혼".equals(trimmed)) {
            return SpecialLeaveOccasionType.CHILD_MARRIAGE;
        }
        if ("배우자출산".equals(trimmed) || "배우자 출산".equals(trimmed) || "출산".equals(trimmed)) {
            return SpecialLeaveOccasionType.SPOUSE_CHILDBIRTH;
        }
        if ("가족사망".equals(trimmed) || "가족 사망".equals(trimmed) || "부고".equals(trimmed) || "장례".equals(trimmed)) {
            return SpecialLeaveOccasionType.FAMILY_DEATH;
        }
        if ("조부모사망".equals(trimmed) || "조부모 사망".equals(trimmed)) {
            return SpecialLeaveOccasionType.GRANDPARENT_DEATH;
        }
        if ("형제자매사망".equals(trimmed) || "형제자매 사망".equals(trimmed)) {
            return SpecialLeaveOccasionType.SIBLING_DEATH;
        }
        if ("기타".equals(trimmed) || "기타경조사".equals(trimmed) || "기타 경조사".equals(trimmed)) {
            return SpecialLeaveOccasionType.OTHER;
        }
        try {
            return SpecialLeaveOccasionType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("경조사 구분을 선택해 주세요.");
        }
    }

    private LocalDate parseRequestDate(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new BusinessException("신청 날짜를 입력해 주세요.");
        }
        String trimmed = rawValue.trim();
        for (DateTimeFormatter formatter : UPLOAD_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported Excel/user input date format.
            }
        }
        throw new BusinessException("신청 날짜 형식이 올바르지 않습니다.");
    }

    private HalfDayType parseHalfDayType(WorkRequestType requestType, String rawValue) {
        if (requestType != WorkRequestType.HALF_DAY) {
            return null;
        }
        String trimmed = rawValue == null ? "" : rawValue.trim();
        String normalized = trimmed.toUpperCase();
        if ("오전".equals(trimmed) || "오전반차".equals(trimmed)) {
            return HalfDayType.MORNING;
        }
        if ("오후".equals(trimmed) || "오후반차".equals(trimmed)) {
            return HalfDayType.AFTERNOON;
        }
        try {
            return HalfDayType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("반차 구분을 선택해 주세요.");
        }
    }

    private Integer normalizeEarlyLeaveMinutes(WorkRequestType requestType, Integer earlyLeaveMinutes) {
        if (requestType != WorkRequestType.EARLY_LEAVE) {
            return null;
        }
        if (earlyLeaveMinutes == null || earlyLeaveMinutes <= 0 || earlyLeaveMinutes % 30 != 0) {
            throw new BusinessException("유연근무 시간은 30분 단위로 입력해 주세요.");
        }
        if (earlyLeaveMinutes > 480) {
            throw new BusinessException("유연근무 시간은 최대 480분까지 입력할 수 있습니다.");
        }
        return earlyLeaveMinutes;
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 500) {
            throw new BusinessException("사유는 500자 이하여야 합니다.");
        }
        return trimmed;
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index <= 6; index++) {
            if (StringUtils.hasText(readCell(row, index))) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row, int cellIndex) {
        if (row == null || row.getCell(cellIndex) == null) {
            return "";
        }
        return EXCEL_DATA_FORMATTER.formatCellValue(row.getCell(cellIndex)).trim();
    }

    private String readDateCell(Row row, int cellIndex) {
        if (row == null || row.getCell(cellIndex) == null) {
            return "";
        }
        if (DateUtil.isCellDateFormatted(row.getCell(cellIndex))) {
            return row.getCell(cellIndex).getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return readCell(row, cellIndex);
    }

    private Integer parseOptionalInteger(String rawValue, String fieldName) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue.trim().replace(".0", ""));
        } catch (NumberFormatException exception) {
            throw new BusinessException(fieldName + "은 숫자로 입력해 주세요.");
        }
    }

    private WorkRequestResponse toResponse(WorkRequest request) {
        return new WorkRequestResponse(
            request.getId(),
            request.getRequestType().name(),
            requestTypeLabel(request.getRequestType()),
            request.getStatus().name(),
            statusLabel(request.getStatus()),
            request.getRequestDate().toString(),
            request.getHalfDayType() == null ? null : request.getHalfDayType().name(),
            halfDayLabel(request.getHalfDayType()),
            request.getOccasionType() == null ? null : request.getOccasionType().name(),
            occasionLabel(request.getOccasionType()),
            request.getEarlyLeaveMinutes(),
            request.getReason(),
            request.getStatus() == WorkRequestStatus.PENDING || request.getStatus() == WorkRequestStatus.APPROVED,
            request.getReviewedBy() == null ? null : request.getReviewedBy().getEmployeeCode(),
            request.getReviewedBy() == null ? null : request.getReviewedBy().getName(),
            formatDateTime(request.getReviewedAt()),
            request.getReviewNote(),
            formatDateTime(request.getCreatedAt())
        );
    }

    private InternalWorkRequestResponse toInternalResponse(WorkRequest request) {
        return new InternalWorkRequestResponse(
            request.getId(),
            request.getEmployee().getId(),
            request.getEmployee().getEmployeeCode(),
            request.getEmployee().getName(),
            request.getEmployee().getWorkplace() == null ? "본사" : request.getEmployee().getWorkplace().getName(),
            request.getRequestType().name(),
            requestTypeLabel(request.getRequestType()),
            request.getStatus().name(),
            statusLabel(request.getStatus()),
            request.getRequestDate().toString(),
            request.getHalfDayType() == null ? null : request.getHalfDayType().name(),
            halfDayLabel(request.getHalfDayType()),
            request.getOccasionType() == null ? null : request.getOccasionType().name(),
            occasionLabel(request.getOccasionType()),
            request.getEarlyLeaveMinutes(),
            request.getReason(),
            request.getReviewedBy() == null ? null : request.getReviewedBy().getEmployeeCode(),
            request.getReviewedBy() == null ? null : request.getReviewedBy().getName(),
            formatDateTime(request.getReviewedAt()),
            request.getReviewNote(),
            formatDateTime(request.getCreatedAt())
        );
    }

    private String requestTypeLabel(WorkRequestType requestType) {
        return switch (requestType) {
            case VACATION -> "휴가";
            case HALF_DAY -> "반차";
            case EARLY_LEAVE -> "유연근무";
            case SPECIAL_LEAVE -> "경조사";
        };
    }

    private String statusLabel(WorkRequestStatus status) {
        return switch (status) {
            case PENDING -> "승인 대기";
            case APPROVED -> "승인";
            case REJECTED -> "반려";
            case CANCEL_REQUESTED -> "취소 요청";
            case CANCELED -> "취소";
        };
    }

    private String halfDayLabel(HalfDayType halfDayType) {
        if (halfDayType == null) {
            return null;
        }
        return halfDayType == HalfDayType.MORNING ? "오전 반차" : "오후 반차";
    }

    private String occasionLabel(SpecialLeaveOccasionType occasionType) {
        if (occasionType == null) {
            return null;
        }
        return switch (occasionType) {
            case SELF_MARRIAGE -> "본인 결혼";
            case CHILD_MARRIAGE -> "자녀 결혼";
            case SPOUSE_CHILDBIRTH -> "배우자 출산";
            case FAMILY_DEATH -> "가족 사망";
            case GRANDPARENT_DEATH -> "조부모 사망";
            case SIBLING_DEATH -> "형제자매 사망";
            case OTHER -> "기타 경조사";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
