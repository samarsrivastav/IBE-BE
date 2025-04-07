package backend.service;

import backend.dto.OTPDto;

import backend.exception.RoomNotAvailableException;

import java.util.UUID;

public interface CancellationService {
    /**
     * Initiates the cancellation process by sending OTP
     * 
     * @param email the email of the guest
     * @param confirmationId the confirmation ID of the booking
     * @return OTP response with status
     * @throws RuntimeException if the booking ID is invalid
     */
    OTPDto.OTPResponse initiateCancellation(String email, UUID confirmationId) throws RuntimeException;

    /**
     * Verifies OTP and processes the cancellation
     * 
     * @param email the email of the guest
     * @param otp the OTP to verify
     * @param confirmationId the confirmation ID of the booking
     * @return OTP verification response with status
     * @throws RoomNotAvailableException if there's an error during cancellation
     */
    OTPDto.OTPVerificationResponse verifyOTPAndCancelBooking(String email, String otp, UUID confirmationId) throws RoomNotAvailableException;
} 