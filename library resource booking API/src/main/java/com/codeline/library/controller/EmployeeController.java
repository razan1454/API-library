package com.codeline.library.controller;

import com.codeline.library.entity.BorrowRecord;
import com.codeline.library.entity.Employee;
import com.codeline.library.entity.Reservation;
import com.codeline.library.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeService.createEmployee(employee);
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{employeeId}/borrows")
    public List<BorrowRecord> getActiveBorrows(@PathVariable Long employeeId) {
        return employeeService.getActiveBorrows(employeeId);
    }

    @GetMapping("/{employeeId}/reservations")
    public List<Reservation> getPendingReservations(@PathVariable Long employeeId) {
        return employeeService.getPendingReservations(employeeId);
    }
}