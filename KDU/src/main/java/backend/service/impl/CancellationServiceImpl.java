package backend.service.impl;

import backend.constants.BookingRoomAvailabilityQueries;
import backend.dto.OTPDto;
import backend.exception.InvalidBookingIdForOTPException;
import backend.exception.RoomNotAvailableException;
import backend.model.BookingTransaction;
import backend.repository.BookingTransactionRepository;
import backend.repository.PseudoBookingRepository;
import backend.service.CancellationService;
import backend.service.OTPService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationServiceImpl implements CancellationService {

    private final OTPService otpService;
    private final BookingTransactionRepository bookingTransactionRepository;
    private final PseudoBookingRepository pseudoBookingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${graphql.api.key}")
    private String apiKey;

    @Value("${graphql.url}")
    private String graphqlEndpoint;

    @Override
    public OTPDto.OTPResponse initiateCancellation(String email, UUID confirmationId) throws InvalidBookingIdForOTPException {
        log.info("=== Starting Cancellation Process ===");
        log.info("Initiating cancellation for booking: {}, email: {}", confirmationId, email);

        try {
            // Verify booking exists and is active
            BookingTransaction bookingTransaction = bookingTransactionRepository.findByConfirmationId(confirmationId)
                    .orElseThrow(() -> new InvalidBookingIdForOTPException("Invalid Booking ID"));

            if (!bookingTransaction.isActive()) {
                throw new InvalidBookingIdForOTPException("Booking is already cancelled");
            }

            // Generate and send OTP
            log.info("Generating and sending OTP for cancellation");
            otpService.generateAndSendOTP(email);
            
            return new OTPDto.OTPResponse("OTP sent successfully for cancellation");
        } catch (Exception e) {
            log.error("Error during cancellation initiation: {}", e.getMessage(), e);
            throw new InvalidBookingIdForOTPException("Failed to initiate cancellation: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OTPDto.OTPVerificationResponse verifyOTPAndCancelBooking(String email, String otp, UUID confirmationId) 
            throws RoomNotAvailableException {
        log.info("=== Starting OTP Verification and Cancellation Process ===");
        log.info("Verifying OTP for booking cancellation: {}", confirmationId);

        try {
            // Verify OTP
            boolean isValid = otpService.verifyOTP(email, otp);
            if (!isValid) {
                log.warn("Invalid OTP provided for cancellation");
                return new OTPDto.OTPVerificationResponse(false, "Invalid OTP");
            }

            // Get the booking transaction
            BookingTransaction bookingTransaction = bookingTransactionRepository.findByConfirmationId(confirmationId)
                    .orElseThrow(() -> new RoomNotAvailableException("Booking not found"));

            if (!bookingTransaction.isActive()) {
                return new OTPDto.OTPVerificationResponse(false, "Booking is already cancelled");
            }

            // Update GraphQL - Reset availability IDs
            log.info("Updating GraphQL - Resetting availability IDs");
            updateAvailabilityIdsInGraphQL(bookingTransaction.getAvailabilityId(), bookingTransaction.getId());

            // Mark booking as inactive and clear availability IDs
            bookingTransaction.setActive(false);
            bookingTransaction.getAvailabilityId().clear();
            bookingTransactionRepository.save(bookingTransaction);

            // Update booking status in GraphQL
            log.info("Updating booking status in GraphQL");
            updateBookingStatusInGraphQL(bookingTransaction.getId());

            // Delete pseudo bookings
            pseudoBookingRepository.deleteByBookingGroupId(bookingTransaction.getId());

            log.info("Booking cancelled successfully: {}", confirmationId);
            return new OTPDto.OTPVerificationResponse(true, "Booking cancelled successfully");
        } catch (Exception e) {
            log.error("Error during cancellation process: {}", e.getMessage(), e);
            throw new RoomNotAvailableException("Failed to cancel booking: " + e.getMessage());
        }
    }

    private void updateAvailabilityIdsInGraphQL(java.util.List<Integer> availabilityIds, long bookingId) throws RoomNotAvailableException {
        log.info("Updating availability IDs in GraphQL for booking: {}", bookingId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        for (Integer availabilityId : availabilityIds) {
            try {
                String mutation = BookingRoomAvailabilityQueries.queryOfUpdatingRoomAvailability(availabilityId, 0);
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("query", mutation);
                
                HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
                
                log.info("Updating availability ID: {}", availabilityId);
                restTemplate.exchange(graphqlEndpoint, HttpMethod.POST, requestEntity, String.class);
            } catch (Exception e) {
                log.error("Error updating availability ID {}: {}", availabilityId, e.getMessage(), e);
                throw new RoomNotAvailableException("Failed to update room availability: " + e.getMessage());
            }
        }
    }

    private void updateBookingStatusInGraphQL(long bookingId)  throws RoomNotAvailableException {
        log.info("Updating booking status in GraphQL for booking: {}", bookingId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        try {
            String mutation = BookingRoomAvailabilityQueries.queryToUpdateTheBookingStatus(bookingId);
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("query", mutation);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("Updating booking status");
            restTemplate.exchange(graphqlEndpoint, HttpMethod.POST, requestEntity, String.class);
        } catch (Exception e) {
            log.error("Error updating booking status: {}", e.getMessage(), e);
            throw new RoomNotAvailableException("Failed to update booking status: " + e.getMessage());
        }
    }
} 