package com.attendance.backend.domain.repository;

import com.attendance.backend.domain.entity.Company;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByName(String name);

    Optional<Company> findByName(String name);

    Optional<Company> findFirstByOrderByIdAsc();
}
