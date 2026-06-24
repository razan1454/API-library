package com.codeline.library.exception;

public class AutoWaitlistedException extends RuntimeException {

    public AutoWaitlistedException(int position) {
        super("Resource is not available and you have been added to the waiting list at position " + position);
    }
}
