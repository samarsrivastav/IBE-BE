package backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmationDetailsDto {
    private String roomName;
    private String startDate;
    private String endDate;
    private long roomTypeId;
    private List<Integer> guestCount;
    private String promotionTitle;
    private int roomCount;
    private int adultCount;
    private int childCount;
    private double totalCost;
    private double amountDueAtResort;
    private long propertyId;
    private double nightlyRate;
    private double subTotal;
    private double taxes;
    private double vat;
}