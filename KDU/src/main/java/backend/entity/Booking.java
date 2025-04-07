package backend.entity;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class Booking {
    private String bookingReference;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int adultsCount;
    private int childrenCount;
    private List<Room> rooms = new ArrayList<>();
    private double totalAmount;
    private double taxAmount;
    private String guestName;
    private String guestEmail;
    private String paymentMethod;
    private String transactionId;
}