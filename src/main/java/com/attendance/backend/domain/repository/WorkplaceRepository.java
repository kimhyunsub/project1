package com.attendance.backend.domain.repository;

import com.attendance.backend.domain.entity.Workplace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {

    boolean existsByCompanyIdAndName(Long companyId, String name);

    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long workplaceId);

    long countByCompanyId(Long companyId);

    List<Workplace> findAllByCompanyIdOrderByNameAsc(Long companyId);

    Optional<Workplace> findByIdAndCompanyId(Long workplaceId, Long companyId);

    Optional<Workplace> findFirstByCompanyIdOrderByIdAsc(Long companyId);
}
