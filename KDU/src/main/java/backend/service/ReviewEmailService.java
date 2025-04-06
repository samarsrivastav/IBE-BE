package backend.service;

import backend.model.BookingTransaction;
import backend.repository.BookingTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewEmailService {

    private final JavaMailSender mailSender;
    private final BookingTransactionRepository bookingTransactionRepository;
    private final ObjectMapper objectMapper;
    private final String appName = "Hotel Booking System";
    private final String apiBaseUrl = "http://localhost:8080"; // Using localhost for testing

    public void sendReviewRequestEmail(BookingTransaction transaction) {
        try {
            String emailContent = buildReviewRequestEmail(transaction);
            
            MimeMessageHelper helper = new MimeMessageHelper(mailSender.createMimeMessage(), true);
            helper.setTo(transaction.getEmail());
            helper.setSubject("How was your stay? - " + appName);
            helper.setText(emailContent, true);
            
            mailSender.send(helper.getMimeMessage());
            System.out.println("Review request email sent successfully to: " + transaction.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send review request email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildReviewRequestEmail(BookingTransaction transaction) {
        try {
            // Convert JsonNode to Map
            Map<String, Object> bookingDetails = objectMapper.convertValue(transaction.getBookingDetails(), Map.class);
            Map<String, Object> confirmationDetails = (Map<String, Object>) bookingDetails.get("confirmationDetails");
            
            String roomName = (String) confirmationDetails.get("roomName");
            String checkInDate = (String) confirmationDetails.get("startDate");
            String checkOutDate = (String) confirmationDetails.get("endDate");
            
            // Handle both Integer and Long roomTypeId
            Object roomTypeIdObj = confirmationDetails.get("roomTypeId");
            Long roomTypeId;
            if (roomTypeIdObj instanceof Integer) {
                roomTypeId = ((Integer) roomTypeIdObj).longValue();
            } else {
                roomTypeId = (Long) roomTypeIdObj;
            }

            // Create a unique token for this review request
            String reviewToken = UUID.randomUUID().toString();

            // Build HTML email with rating form
            return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        * {
                            box-sizing: border-box;
                            margin: 0;
                            padding: 0;
                        }
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            background-color: #f8f9fa;
                            padding: 20px;
                        }
                        .email-container {
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: white;
                            border-radius: 10px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                            overflow: hidden;
                        }
                        .header {
                            background-color: #28a745;
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        .header h2 {
                            margin: 0;
                            font-size: 24px;
                        }
                        .content {
                            padding: 30px;
                        }
                        .room-info {
                            background-color: #f8f9fa;
                            border-radius: 8px;
                            padding: 20px;
                            margin-bottom: 30px;
                        }
                        .room-info p {
                            margin: 10px 0;
                        }
                        .room-info strong {
                            color: #28a745;
                        }
                        .form-group {
                            margin-bottom: 25px;
                        }
                        .rating-options {
                            display: flex;
                            justify-content: center;
                            gap: 15px;
                            margin-top: 15px;
                        }
                        .rating-option {
                            position: relative;
                            width: 50px;
                            height: 50px;
                        }
                        .rating-option input[type="radio"] {
                            position: absolute;
                            opacity: 0;
                            width: 100%%;
                            height: 100%%;
                            cursor: pointer;
                            z-index: 2;
                        }
                        .rating-number {
                            position: relative;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            width: 100%%;
                            height: 100%%;
                            background-color: #f8f9fa;
                            border: 2px solid #dee2e6;
                            border-radius: 50%%;
                            font-size: 20px;
                            font-weight: bold;
                            color: #495057;
                            transition: all 0.3s ease;
                            cursor: pointer;
                        }
                        .rating-option:hover .rating-number {
                            background-color: #e9ecef;
                            border-color: #adb5bd;
                            transform: translateY(-2px);
                        }
                        .rating-option input[type="radio"]:checked + .rating-number {
                            background-color: #28a745;
                            border-color: #28a745;
                            color: white;
                            transform: scale(1.1);
                        }
                        .rating-label {
                            text-align: center;
                            font-weight: bold;
                            margin-bottom: 15px;
                            color: #495057;
                            font-size: 18px;
                        }
                        textarea {
                            width: 100%%;
                            padding: 15px;
                            border: 1px solid #ddd;
                            border-radius: 8px;
                            resize: vertical;
                            min-height: 120px;
                            font-family: inherit;
                            font-size: 16px;
                            transition: border-color 0.3s;
                        }
                        textarea:focus {
                            outline: none;
                            border-color: #28a745;
                        }
                        .submit-btn {
                            background-color: #28a745;
                            color: white;
                            padding: 15px 30px;
                            border: none;
                            border-radius: 8px;
                            cursor: pointer;
                            font-size: 16px;
                            font-weight: bold;
                            transition: all 0.3s;
                            width: 100%%;
                            margin-top: 20px;
                        }
                        .submit-btn:hover {
                            background-color: #218838;
                            transform: translateY(-2px);
                        }
                        .submit-btn:active {
                            transform: translateY(0);
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            background-color: #f8f9fa;
                            border-top: 1px solid #dee2e6;
                        }
                        .footer p {
                            margin: 5px 0;
                            color: #6c757d;
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="header">
                            <h2>How was your stay?</h2>
                        </div>
                        <div class="content">
                            <p>Dear Guest,</p>
                            
                            <div class="room-info">
                                <p>Thank you for staying with us at <strong>%s</strong></p>
                                <p>Check-in: %s</p>
                                <p>Check-out: %s</p>
                            </div>
                            
                            <p>We would love to hear about your experience!</p>
                            
                            <form action="%s/api/room-types/ratings" method="POST">
                                <input type="hidden" name="token" value="%s">
                                <input type="hidden" name="roomTypeId" value="%d">
                                <input type="hidden" name="email" value="%s">
                                
                                <div class="form-group">
                                    <div class="rating-label">Rate your stay:</div>
                                    <div class="rating-options">
                                        <label class="rating-option">
                                            <input type="radio" name="rating" value="1" required>
                                            <span class="rating-number">1</span>
                                        </label>
                                        <label class="rating-option">
                                            <input type="radio" name="rating" value="2">
                                            <span class="rating-number">2</span>
                                        </label>
                                        <label class="rating-option">
                                            <input type="radio" name="rating" value="3">
                                            <span class="rating-number">3</span>
                                        </label>
                                        <label class="rating-option">
                                            <input type="radio" name="rating" value="4">
                                            <span class="rating-number">4</span>
                                        </label>
                                        <label class="rating-option">
                                            <input type="radio" name="rating" value="5">
                                            <span class="rating-number">5</span>
                                        </label>
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <label style="display: block; margin-bottom: 10px; font-weight: bold;">Your review (optional):</label>
                                    <textarea name="reviewText" placeholder="Share your experience with us..."></textarea>
                                </div>
                                
                                <button type="submit" class="submit-btn">
                                    Submit Review
                                </button>
                            </form>
                        </div>
                        <div class="footer">
                            <p>Thank you for your feedback!</p>
                            <p>Best regards,<br>%s Team</p>
                        </div>
                    </div>
                </body>
                </html>
                """, roomName, checkInDate, checkOutDate, apiBaseUrl, reviewToken, roomTypeId, transaction.getEmail(), appName);
        } catch (Exception e) {
            System.err.println("Error building review email: " + e.getMessage());
            e.printStackTrace();
            return "Error building email content";
        }
    }
} 