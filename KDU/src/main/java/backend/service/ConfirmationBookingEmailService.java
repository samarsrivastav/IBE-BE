package backend.service;

import java.util.UUID;

public interface ConfirmationBookingEmailService {
    void emailConfirmationDetailsOfBooking(UUID confirmationId, String email);
} 