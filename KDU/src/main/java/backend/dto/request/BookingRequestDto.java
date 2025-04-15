package backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto 
{
    private ConfirmationDetailsDto confirmationDetails;
    private CheckoutRequestDTO.BillingInfoDTO billingInfo;
    private CheckoutRequestDTO.PaymentInfoDTO paymentInfo;
    private CheckoutRequestDTO.TravelerInfoDTO travelerInfo;
    private int currentIndex;
    private String imageUrl;
    private boolean termsAndPolicies;

    @JsonProperty("specialOffers")
    private boolean specialOffers;
} 