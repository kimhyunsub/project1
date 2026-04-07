package com.attendance.backend.domain.repository;

import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.EmployeeRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

    long countByCompanyIdAndDeletedFalseAndRoleNot(Long companyId, EmployeeRole role);

    @EntityGraph(attributePaths = {"company", "workplace"})
    Optional<Employee> findByEmployeeCode(String employeeCode);

    @EntityGraph(attributePaths = {"company", "workplace"})
    java.util.List<Employee> findAllByCompanyIdOrderByNameAsc(Long companyId);

    @EntityGraph(attributePaths = {"company", "workplace"})
    java.util.List<Employee> findAllByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);

    @EntityGraph(attributePaths = {"company", "workplace"})
    java.util.List<Employee> findAllByCompanyIdAndDeletedFalseOrderByNameAsc(Long companyId);

    @EntityGraph(attributePaths = {"company", "workplace"})
    java.util.List<Employee> findAllByCompanyIdAndActiveTrueAndDeletedFalseOrderByNameAsc(Long companyId);
}
