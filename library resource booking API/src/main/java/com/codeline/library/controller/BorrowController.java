package com.codeline.library.controller;

import com.codeline.library.entity.BorrowRecord;
import com.codeline.library.exception.AutoWaitlistedException;
import com.codeline.library.service.BorrowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    public ResponseEntity<?> borrowResource(
            @RequestParam Long employeeId,
            @RequestParam Long resourceId) {

        try {
            return ResponseEntity.ok(borrowService.borrowResource(employeeId, resourceId));
        } catch (AutoWaitlistedException ex) {
            return ResponseEntity.accepted().body(ex.getMessage());
        }
    }

    @PostMapping("/return")
    public ResponseEntity<String> returnResource(
            @RequestParam Long borrowRecordId) {

        borrowService.returnResource(borrowRecordId);
        return ResponseEntity.ok("Thank you for returning the resource");
    }
}