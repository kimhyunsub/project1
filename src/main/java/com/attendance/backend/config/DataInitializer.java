package com.attendance.backend.config;

import com.attendance.backend.domain.entity.Company;
import com.attendance.backend.domain.entity.CompanyPlan;
import com.attendance.backend.domain.entity.CompanySetting;
import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.EmployeeRole;
import com.attendance.backend.domain.repository.CompanyRepository;
import com.attendance.backend.domain.repository.CompanySettingRepository;
import com.attendance.backend.domain.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalTime;

@Configuration
public class DataInitializer {
    private static final int DEFAULT_FREE_EMPLOYEE_LIMIT = 7;
    private static final int DEFAULT_FREE_WORKPLACE_LIMIT = 0;

    @Bean
    CommandLineRunner seedData(
        CompanyRepository companyRepository,
        CompanySettingRepository companySettingRepository,
        EmployeeRepository employeeRepository,
        PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (employeeRepository.count() > 0) {
                return;
            }

            Company company = companyRepository.save(
                new Company(
                    "OpenAI Seoul Office",
                    CompanyPlan.FREE,
                    DEFAULT_FREE_EMPLOYEE_LIMIT,
                    DEFAULT_FREE_WORKPLACE_LIMIT,
                    37.5665,
                    126.9780
                )
            );

            companySettingRepository.save(new CompanySetting(company, 100, LocalTime.of(9, 0)));

            employeeRepository.save(
                new Employee(
                    "ADMIN001",
                    "관리자",
                    passwordEncoder.encode("admin1234"),
                    EmployeeRole.ADMIN,
                    company
                )
            );

            employeeRepository.save(
                new Employee(
                    "EMP001",
                    "홍길동",
                    passwordEncoder.encode("password1234"),
                    EmployeeRole.EMPLOYEE,
                    company
                )
            );
        };
    }
}
