package com.cryptocraft.identityservice.exceptions;

/**
 * The change would break the venue's staff or the account (409): the person is already there, the last
 * admin would be removed, a wrong current password...
 */
public class StaffConflictException extends RuntimeException {

    public StaffConflictException(String message) {
        super(message);
    }
}
