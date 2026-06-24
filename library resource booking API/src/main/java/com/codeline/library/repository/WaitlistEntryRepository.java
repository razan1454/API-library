package com.codeline.library.repository;

import com.codeline.library.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    WaitlistEntry findTopByResourceIdOrderByPositionDesc(Long resourceId);

    WaitlistEntry findTopByResourceIdAndActiveTrueOrderByPositionAsc(Long resourceId);

    boolean existsByEmployeeIdAndResourceIdAndActiveTrue(Long employeeId, Long resourceId);

    Optional<WaitlistEntry> findByEmployeeIdAndResourceIdAndActiveTrue(Long employeeId, Long resourceId);
}