package backend.dto.request;

import backend.dto.OTPDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingVerificationRequestDto {
    private OTPDto.OTPVerificationRequest otpRequest;
    private BookingRequestDto bookingRequestDto;
} 