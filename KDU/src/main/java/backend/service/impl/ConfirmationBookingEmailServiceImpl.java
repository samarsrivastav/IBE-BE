package backend.service.impl;

import backend.service.ConfirmationBookingEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmationBookingEmailServiceImpl implements ConfirmationBookingEmailService {
    
    @Override
    public void emailConfirmationDetailsOfBooking(UUID confirmationId, String email) {
        log.info("Sending booking confirmation email for ID: {} to: {}", confirmationId, email);
      
    }
} 