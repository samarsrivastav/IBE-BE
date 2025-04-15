package backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TenantNotAllowedException extends RuntimeException {
    public TenantNotAllowedException(String message) {
        super(message);
    }
}
