package com.attendance.backend.config;

import com.attendance.backend.domain.entity.AttendanceRecord;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.repository.AttendanceRecordRepository;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AttendanceLateStatusBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AttendanceLateStatusBackfillRunner.class);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CompanySettingRepository companySettingRepository;

    public AttendanceLateStatusBackfillRunner(
        AttendanceRecordRepository attendanceRecordRepository,
        CompanySettingRepository companySettingRepository
    ) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.companySettingRepository = companySettingRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<AttendanceRecord> lateRecords = attendanceRecordRepository.findAllByLateTrue();
        if (lateRecords.isEmpty()) {
            return;
        }

        Map<Long, LocalTime> companyLateAfterTimes = new HashMap<>();
        int correctedCount = 0;

        for (AttendanceRecord record : lateRecords) {
            Employee employee = record.getEmployee();
            LocalTime lateReferenceTime = employee.getWorkStartTime();

            if (lateReferenceTime == null) {
                Long companyId = employee.getCompany().getId();
                lateReferenceTime = companyLateAfterTimes.computeIfAbsent(companyId, key ->
                    companySettingRepository.findByCompany(employee.getCompany())
                        .map(CompanySetting::getLateAfterTime)
                        .orElse(null)
                );
            }

            if (lateReferenceTime == null) {
                continue;
            }

            boolean shouldRemainLate = record.getCheckInTime()
                .toLocalTime()
                .truncatedTo(ChronoUnit.MINUTES)
                .isAfter(lateReferenceTime.truncatedTo(ChronoUnit.MINUTES));

            if (!shouldRemainLate) {
                record.updateLate(false);
                correctedCount++;
            }
        }

        if (correctedCount > 0) {
            log.info("Corrected {} attendance records from late to on-time using minute-based lateness rules.", correctedCount);
        }
    }
}
