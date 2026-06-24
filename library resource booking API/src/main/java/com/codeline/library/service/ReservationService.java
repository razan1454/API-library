package com.codeline.library.service;

import com.codeline.library.entity.WaitlistEntry;
import com.codeline.library.repository.BorrowRecordRepository;
import com.codeline.library.repository.ReservationRepository;
import com.codeline.library.repository.WaitlistEntryRepository;
import org.springframework.stereotype.Service;
import com.codeline.library.entity.BorrowRecord;
import com.codeline.library.entity.Reservation;
import com.codeline.library.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            BorrowRecordRepository borrowRecordRepository,
            WaitlistEntryRepository waitlistEntryRepository) {

        this.reservationRepository = reservationRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    public Reservation claimReservation(Long reservationId, Long employeeId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!reservation.getEmployee().getId().equals(employeeId)) {
            throw new RuntimeException(
                    "You cannot claim another employee's reservation"
            );
        }

        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {

            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            // automatically advance the queue for the next person
            advanceQueue(reservation.getResource().getId());

            throw new RuntimeException("Reservation expired");
        }

        reservation.setStatus(ReservationStatus.CLAIMED);

        BorrowRecord borrowRecord = new BorrowRecord();

        borrowRecord.setEmployee(reservation.getEmployee());
        borrowRecord.setResource(reservation.getResource());

        borrowRecord.setBorrowedAt(LocalDateTime.now());
        borrowRecord.setActive(true);

        borrowRecordRepository.save(borrowRecord);

        return reservationRepository.save(reservation);
    }

    // called by BorrowService before the availability check to release expired holds
    public void processExpiredReservations(Long resourceId) {
        List<Reservation> expired = reservationRepository
                .findByResourceIdAndStatusAndExpiresAtBefore(
                        resourceId, ReservationStatus.PENDING, LocalDateTime.now());
        cascade(expired);
    }

    // called by POST /reservations/process-expired (manual global trigger)
    public List<Reservation> processExpiredReservations() {
        List<Reservation> expired = reservationRepository
                .findAllByStatusAndExpiresAtBefore(ReservationStatus.PENDING, LocalDateTime.now());
        return cascade(expired);
    }

    private List<Reservation> cascade(List<Reservation> expired) {
        List<Reservation> created = new ArrayList<>();

        for (Reservation reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            Reservation next = advanceQueue(reservation.getResource().getId());
            if (next != null) {
                created.add(next);
            }
        }

        return created;
    }

    // finds the next person in the waitlist for a resource and gives them a fresh reservation
    private Reservation advanceQueue(Long resourceId) {
        WaitlistEntry next = waitlistEntryRepository
                .findTopByResourceIdAndActiveTrueOrderByPositionAsc(resourceId);

        if (next == null) {
            return null;
        }

        Reservation newReservation = new Reservation();
        newReservation.setEmployee(next.getEmployee());
        newReservation.setResource(next.getResource());
        newReservation.setStatus(ReservationStatus.PENDING);
        newReservation.setReservedAt(LocalDateTime.now());
        newReservation.setExpiresAt(LocalDateTime.now().plusHours(2));

        Reservation saved = reservationRepository.save(newReservation);

        next.setActive(false);
        waitlistEntryRepository.save(next);

        return saved;
    }
}
