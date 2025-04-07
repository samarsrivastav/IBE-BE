package backend.controller;

import backend.dto.request.RoomTypeRatingRequestDTO;
import backend.model.BookingTransaction;
import backend.entity.RoomTypes;
import backend.repository.BookingTransactionRepository;
import backend.service.RoomTypeRatingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/room-types/ratings")
@RequiredArgsConstructor
public class RoomTypeRatingController {

    private final RoomTypeRatingService roomTypeRatingService;
    private final BookingTransactionRepository bookingTransactionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping
    public String updateRating(
            @RequestParam("token") String token,
            @RequestParam("roomTypeId") Long roomTypeId,
            @RequestParam("email") String email,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "reviewText", required = false) String reviewText,
            @RequestParam("checkInDate") String checkInDate,
            @RequestParam("checkOutDate") String checkOutDate) {
        
        // Check if already reviewed
        Optional<List<BookingTransaction>> transactionsOptional = bookingTransactionRepository.findByEmail(email);
        List<BookingTransaction> transactions = transactionsOptional.orElse(null);
        BookingTransaction transaction = null;
        
        if (transactions != null && !transactions.isEmpty()) {
            // Find the transaction that matches the roomTypeId AND dates
            for (BookingTransaction t : transactions) {
                if (t.getBookingDetails() != null) {
                    JsonNode confirmationDetails = t.getBookingDetails().get("confirmationDetails");
                    if (confirmationDetails != null && 
                        confirmationDetails.has("roomTypeId") && 
                        confirmationDetails.has("startDate") && 
                        confirmationDetails.has("endDate")) {
                        
                        JsonNode roomTypeIdNode = confirmationDetails.get("roomTypeId");
                        String bookingStartDate = confirmationDetails.get("startDate").asText();
                        String bookingEndDate = confirmationDetails.get("endDate").asText();
                        
                        Long transactionRoomTypeId;
                        if (roomTypeIdNode.isInt()) {
                            transactionRoomTypeId = roomTypeIdNode.asLong();
                        } else if (roomTypeIdNode.isLong()) {
                            transactionRoomTypeId = roomTypeIdNode.asLong();
                        } else {
                            continue; // Skip if roomTypeId is not a number
                        }
                        
                        // Match both roomTypeId and dates
                        if (transactionRoomTypeId.equals(roomTypeId) && 
                            bookingStartDate.equals(checkInDate) && 
                            bookingEndDate.equals(checkOutDate)) {
                            transaction = t;
                            break;
                        }
                    }
                }
            }
        }
            
        if (transaction == null) {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Booking Not Found</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            text-align: center;
                            background-color: #f8f9fa;
                        }
                        .message {
                            background-color: white;
                            padding: 40px;
                            border-radius: 10px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        h1 {
                            color: #dc3545;
                            margin-bottom: 20px;
                        }
                        p {
                            color: #6c757d;
                            font-size: 18px;
                            line-height: 1.6;
                        }
                        .emoji {
                            font-size: 48px;
                            margin-bottom: 20px;
                        }
                    </style>
                </head>
                <body>
                    <div class="message">
                   
                        <h1>Booking Not Found</h1>
                        <p>We couldn't find a matching booking with the provided details.</p>
                        <p>Please ensure you're using the correct link from your email.</p>
                    </div>
                </body>
                </html>
                """;
        }

        if (transaction.isHasReviewed()) {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Already Reviewed</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            text-align: center;
                            background-color: #f8f9fa;
                        }
                        .message {
                            background-color: white;
                            padding: 40px;
                            border-radius: 10px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        h1 {
                            color: #dc3545;
                            margin-bottom: 20px;
                        }
                        p {
                            color: #6c757d;
                            font-size: 18px;
                            line-height: 1.6;
                        }
                        .emoji {
                            font-size: 48px;
                            margin-bottom: 20px;
                        }
                    </style>
                </head>
                <body>
                    <div class="message">
                      
                        <h1>Already Reviewed</h1>
                        <p>You have already submitted a review for this stay.</p>
                        <p>Thank you for your feedback!</p>
                    </div>
                </body>
                </html>
                """;
        }
        
        RoomTypeRatingRequestDTO requestDTO = new RoomTypeRatingRequestDTO();
        requestDTO.setToken(token);
        requestDTO.setRoomTypeId(roomTypeId);
        requestDTO.setEmail(email);
        requestDTO.setRating(Double.valueOf(rating));
        requestDTO.setReviewText(reviewText);
        
        RoomTypes updatedRoom = roomTypeRatingService.updateRating(requestDTO);
        
        // Update the hasReviewed flag
        transaction.setHasReviewed(true);
        bookingTransactionRepository.save(transaction);
        
        // Return HTML thank you page
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Thank You</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        text-align: center;
                        background-color: #f8f9fa;
                    }
                    .thank-you {
                        background-color: white;
                        padding: 40px;
                        border-radius: 10px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    h1 {
                        color: #28a745;
                        margin-bottom: 20px;
                    }
                    p {
                        color: #6c757d;
                        font-size: 18px;
                        line-height: 1.6;
                    }
                    .emoji {
                        font-size: 48px;
                        margin-bottom: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="thank-you">
                    
                    <h1>Thank You for Your Review</h1>
                    <p>We appreciate your feedback and value your input. Your review helps us improve our services.</p>
                    <p>We hope to see you again soon!</p>
                </div>
            </body>
            </html>
            """;
    }
} 