package backend.exception;

public class InvalidBookingIdForOTPException extends RuntimeException {
    public InvalidBookingIdForOTPException(String message) {
        super(message);
    }

    public InvalidBookingIdForOTPException(String message, Throwable cause) {
        super(message, cause);
    }
} 