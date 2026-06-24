package com.codeline.library.service;

import com.codeline.library.enums.ReservationStatus;
import com.codeline.library.entity.Reservation;
import com.codeline.library.entity.WaitlistEntry;
import com.codeline.library.entity.BorrowRecord;
import com.codeline.library.entity.Employee;
import com.codeline.library.entity.Resource;
import com.codeline.library.exception.AutoWaitlistedException;
import com.codeline.library.repository.BorrowRecordRepository;
import com.codeline.library.repository.EmployeeRepository;
import com.codeline.library.repository.ResourceRepository;
import com.codeline.library.repository.ReservationRepository;
import com.codeline.library.repository.WaitlistEntryRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BorrowService {

    private final EmployeeRepository employeeRepository;
    private final ResourceRepository resourceRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public BorrowService(
            EmployeeRepository employeeRepository,
            ResourceRepository resourceRepository,
            BorrowRecordRepository borrowRecordRepository,
            WaitlistEntryRepository waitlistEntryRepository,
            ReservationRepository reservationRepository,
            @Lazy ReservationService reservationService) {

        this.employeeRepository = employeeRepository;
        this.resourceRepository = resourceRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    public BorrowRecord borrowResource(Long employeeId, Long resourceId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        boolean alreadyBorrowed =
                borrowRecordRepository
                        .existsByEmployeeIdAndResourceIdAndActiveTrue(
                                employeeId,
                                resourceId
                        );

        if (alreadyBorrowed) {
            throw new RuntimeException(
                    "Employee already has this resource borrowed"
            );
        }

        // release any expired holds before checking availability
        reservationService.processExpiredReservations(resourceId);

        long activeBorrows =
                borrowRecordRepository.countByResourceIdAndActiveTrue(resourceId);

        long pendingReservations =
                reservationRepository.countByResourceIdAndStatus(
                        resourceId,
                        ReservationStatus.PENDING
                );

        if (activeBorrows + pendingReservations >= resource.getTotalCopies()) {

            boolean alreadyWaiting =
                    waitlistEntryRepository
                            .existsByEmployeeIdAndResourceIdAndActiveTrue(employeeId, resourceId);

            if (alreadyWaiting) {
                throw new RuntimeException(
                        "Employee is already in the waitlist for this resource"
                );
            }

            WaitlistEntry last =
                    waitlistEntryRepository.findTopByResourceIdOrderByPositionDesc(resourceId);

            int nextPosition = (last != null) ? last.getPosition() + 1 : 1;

            WaitlistEntry entry = new WaitlistEntry();
            entry.setEmployee(employee);
            entry.setResource(resource);
            entry.setJoinedAt(LocalDateTime.now());
            entry.setPosition(nextPosition);
            entry.setActive(true);

            waitlistEntryRepository.save(entry);
            throw new AutoWaitlistedException(nextPosition);
        }

        BorrowRecord borrowRecord = new BorrowRecord();

        borrowRecord.setEmployee(employee);
        borrowRecord.setResource(resource);
        borrowRecord.setBorrowedAt(LocalDateTime.now());
        borrowRecord.setActive(true);

        return borrowRecordRepository.save(borrowRecord);
    }

    public BorrowRecord returnResource(Long borrowRecordId) {

        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new RuntimeException("Borrow record not found"));

        borrowRecord.setActive(false);
        borrowRecord.setReturnedAt(LocalDateTime.now());

        BorrowRecord savedRecord =
                borrowRecordRepository.save(borrowRecord);

        WaitlistEntry firstInQueue =
                waitlistEntryRepository
                        .findTopByResourceIdAndActiveTrueOrderByPositionAsc(
                                borrowRecord.getResource().getId()
                        );

        if (firstInQueue != null) {

            Reservation reservation = new Reservation();

            reservation.setEmployee(firstInQueue.getEmployee());
            reservation.setResource(firstInQueue.getResource());

            reservation.setStatus(ReservationStatus.PENDING);

            reservation.setReservedAt(LocalDateTime.now());

            reservation.setExpiresAt(
                    LocalDateTime.now().plusHours(24)
            );

            reservationRepository.save(reservation);
            firstInQueue.setActive(false);
            waitlistEntryRepository.save(firstInQueue);
        }

        return savedRecord;
    }
}
