package com.akshit.comefort.exception;

/**
 * Base exception for all ComeFort application errors.
 * All custom exceptions extend this to allow catch-all handling at the CLI layer.
 */
public class ComeFortException extends RuntimeException {

    public ComeFortException(String message) {
        super(message);
    }

    public ComeFortException(String message, Throwable cause) {
        super(message, cause);
    }
}
