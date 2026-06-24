package com.codeline.library.controller;

import com.codeline.library.entity.WaitlistEntry;
import com.codeline.library.service.WaitlistService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public WaitlistEntry joinWaitlist(
            @RequestParam Long employeeId,
            @RequestParam Long resourceId) {

        return waitlistService.joinWaitlist(employeeId, resourceId);
    }

    @DeleteMapping
    public WaitlistEntry leaveWaitlist(
            @RequestParam Long employeeId,
            @RequestParam Long resourceId) {

        return waitlistService.leaveWaitlist(employeeId, resourceId);
    }
}