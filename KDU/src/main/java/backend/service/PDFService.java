package backend.service;
import backend.model.BookingTransaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PDFService {

    private final ObjectMapper objectMapper;

    public byte[] generateBookingInvoice(BookingTransaction transaction) {
        log.info("Generating PDF invoice for booking: {}", transaction.getConfirmationId());
        
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

            // Convert JsonNode to Map
            Map<String, Object> bookingDetails = objectMapper.convertValue(transaction.getBookingDetails(), Map.class);
            
            Map<String, Object> confirmationDetails = (Map<String, Object>) bookingDetails.get("confirmationDetails");
            Map<String, Object> billingInfo = (Map<String, Object>) bookingDetails.get("billingInfo");

            String guestName = billingInfo.get("firstName") + " " + billingInfo.get("lastName");
            String guestEmail = (String) billingInfo.get("email");

            String roomType = (String) confirmationDetails.get("roomName");
            int roomCount = (int) confirmationDetails.get("roomCount");
            double rate = ((Number) confirmationDetails.get("nightlyRate")).doubleValue();
            double taxes = ((Number) confirmationDetails.get("taxes")).doubleValue();
            double vat = ((Number) confirmationDetails.get("vat")).doubleValue();
            double totalCost = ((Number) confirmationDetails.get("totalCost")).doubleValue();
            String checkIn = (String) confirmationDetails.get("startDate");
            String checkOut = (String) confirmationDetails.get("endDate");

            LocalDate checkInDate = LocalDate.parse(checkIn);
            LocalDate checkOutDate = LocalDate.parse(checkOut);
            long nights = java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);

            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Booking Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            // Invoice Info
            document.add(new Paragraph("Invoice #: " + transaction.getConfirmationId(), boldFont));
            document.add(new Paragraph("Date: " + LocalDate.now().format(dateFormatter), normalFont));
            document.add(Chunk.NEWLINE);

            // Guest Info
            document.add(new Paragraph("Customer Information:", boldFont));
            document.add(new Paragraph("Name: " + guestName, normalFont));
            document.add(new Paragraph("Email: " + guestEmail, normalFont));
            document.add(Chunk.NEWLINE);

            // Stay Info
            document.add(new Paragraph("Stay Details:", boldFont));
            document.add(new Paragraph("Check-in: " + checkInDate.format(dateFormatter), normalFont));
            document.add(new Paragraph("Check-out: " + checkOutDate.format(dateFormatter), normalFont));
            document.add(new Paragraph("Nights: " + nights, normalFont));
            document.add(new Paragraph("Guests: " + confirmationDetails.get("adultCount") + " adults, "
                    + confirmationDetails.get("childCount") + " children", normalFont));
            document.add(Chunk.NEWLINE);

            // Room Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1f, 1f, 1.5f});

            Stream.of("Room Type", "Rooms", "Rate/Night", "Total")
                    .forEach(col -> {
                        PdfPCell cell = new PdfPCell(new Phrase(col, boldFont));
                        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        table.addCell(cell);
                    });

            double subtotal = rate * nights * roomCount;

            table.addCell(new Phrase(roomType, normalFont));
            table.addCell(new Phrase(String.valueOf(roomCount), normalFont));
            table.addCell(new Phrase(currencyFormatter.format(rate), normalFont));
            table.addCell(new Phrase(currencyFormatter.format(subtotal), normalFont));

            document.add(table);

            // Totals
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            totalTable.addCell(new PdfPCell(new Phrase("Subtotal", boldFont)));
            totalTable.addCell(new PdfPCell(new Phrase(currencyFormatter.format(subtotal), normalFont)));

            totalTable.addCell(new PdfPCell(new Phrase("Taxes", boldFont)));
            totalTable.addCell(new PdfPCell(new Phrase(currencyFormatter.format(taxes), normalFont)));

            totalTable.addCell(new PdfPCell(new Phrase("VAT", boldFont)));
            totalTable.addCell(new PdfPCell(new Phrase(currencyFormatter.format(vat), normalFont)));

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total", boldFont));
            PdfPCell totalValue = new PdfPCell(new Phrase(currencyFormatter.format(totalCost), boldFont));
            totalLabel.setBackgroundColor(BaseColor.LIGHT_GRAY);
            totalValue.setBackgroundColor(BaseColor.LIGHT_GRAY);
            totalTable.addCell(totalLabel);
            totalTable.addCell(totalValue);

            document.add(totalTable);

            // Footer
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Thank you for choosing us!", boldFont));
            document.add(new Paragraph("If you have any questions, please contact support@example.com", normalFont));

            document.close();
            writer.close();

            log.info("PDF invoice generated successfully for booking: {}", transaction.getConfirmationId());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF invoice: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

}