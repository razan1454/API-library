package com.codeline.library.repository;

import com.codeline.library.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    long countByResourceIdAndActiveTrue(Long resourceId);

    boolean existsByEmployeeIdAndResourceIdAndActiveTrue(Long employeeId, Long resourceId);

    List<BorrowRecord> findByEmployeeIdAndActiveTrue(Long employeeId);
}