package com.codeline.library.repository;

import com.codeline.library.entity.Reservation;
import com.codeline.library.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByResourceIdAndStatus(Long resourceId, ReservationStatus status);

    List<Reservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime now);

    List<Reservation> findByResourceIdAndStatusAndExpiresAtBefore(Long resourceId, ReservationStatus status, LocalDateTime now);

    List<Reservation> findByEmployeeIdAndStatus(Long employeeId, ReservationStatus status);
}