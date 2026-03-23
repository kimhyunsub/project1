package com.attendance.backend.domain.repository;

import com.attendance.backend.domain.entity.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @EntityGraph(attributePaths = {"company"})
    Optional<Employee> findByEmployeeCode(String employeeCode);

    @EntityGraph(attributePaths = {"company"})
    java.util.List<Employee> findAllByCompanyIdOrderByNameAsc(Long companyId);

    @EntityGraph(attributePaths = {"company"})
    java.util.List<Employee> findAllByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);
}
