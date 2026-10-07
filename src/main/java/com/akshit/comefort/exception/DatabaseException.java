package com.akshit.comefort.exception;

/**
 * Thrown when a database operation fails — wraps SQLExceptions
 * with a user-friendly message.
 */
public class DatabaseException extends ComeFortException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
