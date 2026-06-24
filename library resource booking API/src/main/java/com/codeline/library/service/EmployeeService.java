package com.codeline.library.service;

import com.codeline.library.entity.BorrowRecord;
import com.codeline.library.entity.Employee;
import com.codeline.library.entity.Reservation;
import com.codeline.library.enums.ReservationStatus;
import com.codeline.library.repository.BorrowRecordRepository;
import com.codeline.library.repository.EmployeeRepository;
import com.codeline.library.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            BorrowRecordRepository borrowRecordRepository,
            ReservationRepository reservationRepository) {

        this.employeeRepository = employeeRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.reservationRepository = reservationRepository;
    }

    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<BorrowRecord> getActiveBorrows(Long employeeId) {
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return borrowRecordRepository.findByEmployeeIdAndActiveTrue(employeeId);
    }

    public List<Reservation> getPendingReservations(Long employeeId) {
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return reservationRepository.findByEmployeeIdAndStatus(employeeId, ReservationStatus.PENDING);
    }
}