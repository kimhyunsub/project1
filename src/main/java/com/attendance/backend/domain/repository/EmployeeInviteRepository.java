package com.attendance.backend.domain.repository;

import com.attendance.backend.domain.entity.EmployeeInvite;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeInviteRepository extends JpaRepository<EmployeeInvite, Long> {

    @EntityGraph(attributePaths = {"employee", "employee.company", "employee.workplace"})
    Optional<EmployeeInvite> findByToken(String token);
}
