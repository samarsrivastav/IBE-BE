package backend.exception;

public class RoomNotAvailableException extends Exception {
    public RoomNotAvailableException(String message) {
        super(message);
    }
    
    public RoomNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
} 