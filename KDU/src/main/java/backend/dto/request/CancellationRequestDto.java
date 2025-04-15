package backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRequestDto {
    private String email;
    private UUID confirmationId;
    private String otp; // This will be null for initiation request
} 