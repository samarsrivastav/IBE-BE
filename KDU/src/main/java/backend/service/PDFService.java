package backend.service;

import backend.entity.Booking;
import backend.entity.Room;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class PDFService {

    public byte[] generateBookingInvoice(Booking booking) {
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

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

            // Invoice details
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("Invoice #: " + booking.getBookingReference(), boldFont));
            document.add(new Paragraph("Date: " + LocalDate.now().format(dateFormatter), normalFont));
            document.add(new Paragraph("\n", normalFont));

            // Customer Info
            document.add(new Paragraph("Customer Information:", boldFont));
            document.add(new Paragraph("Name: " + booking.getGuestName(), normalFont));
            document.add(new Paragraph("Email: " + booking.getGuestEmail(), normalFont));
            document.add(new Paragraph("\n", normalFont));

            // Booking details
            document.add(new Paragraph("Booking Details:", boldFont));
            document.add(new Paragraph("Check-in: " + booking.getCheckInDate().format(dateFormatter), normalFont));
            document.add(new Paragraph("Check-out: " + booking.getCheckOutDate().format(dateFormatter), normalFont));
            document.add(new Paragraph("Guests: " + booking.getAdultsCount() + " adults, " + booking.getChildrenCount() + " children", normalFont));
            document.add(new Paragraph("\n", normalFont));

            // Room details table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            float[] columnWidths = {1.5f, 1f, 1f, 1.5f};
            table.setWidths(columnWidths);

            // Table headers
            PdfPCell cell1 = new PdfPCell(new Phrase("Room Type", boldFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("Nights", boldFont));
            PdfPCell cell3 = new PdfPCell(new Phrase("Rate/Night", boldFont));
            PdfPCell cell4 = new PdfPCell(new Phrase("Total", boldFont));

            cell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell3.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell4.setBackgroundColor(BaseColor.LIGHT_GRAY);

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
            table.addCell(cell4);

            // Calculate nights
            long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());

            // Room details rows
            for (Room room : booking.getRooms()) {
                table.addCell(new Phrase(room.getRoomType(), normalFont));
                table.addCell(new Phrase(String.valueOf(nights), normalFont));
                table.addCell(new Phrase(currencyFormatter.format(room.getPrice()), normalFont));
                table.addCell(new Phrase(currencyFormatter.format(room.getPrice() * nights), normalFont));
            }

            document.add(table);

            // Total
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            cell1 = new PdfPCell(new Phrase("Subtotal", boldFont));
            cell2 = new PdfPCell(new Phrase(currencyFormatter.format(booking.getTotalAmount()), normalFont));

            totalTable.addCell(cell1);
            totalTable.addCell(cell2);

            // Add tax if applicable
            if (booking.getTaxAmount() > 0) {
                cell1 = new PdfPCell(new Phrase("Tax", boldFont));
                cell2 = new PdfPCell(new Phrase(currencyFormatter.format(booking.getTaxAmount()), normalFont));
                totalTable.addCell(cell1);
                totalTable.addCell(cell2);
            }

            Font totalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            cell1 = new PdfPCell(new Phrase("Total", totalFont));
            cell2 = new PdfPCell(new Phrase(currencyFormatter.format(booking.getTotalAmount() + booking.getTaxAmount()), totalFont));
            cell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell2.setBackgroundColor(BaseColor.LIGHT_GRAY);

            totalTable.addCell(cell1);
            totalTable.addCell(cell2);
            document.add(totalTable);

            // Payment information
            document.add(new Paragraph("\nPayment Information:", boldFont));
            document.add(new Paragraph("Payment Method: " + booking.getPaymentMethod(), normalFont));
            document.add(new Paragraph("Payment Status: Paid", normalFont));
            document.add(new Paragraph("Transaction ID: " + booking.getTransactionId(), normalFont));

            // Footer
            document.add(new Paragraph("\n\nThank you for your booking!", boldFont));
            document.add(new Paragraph("For any inquiries, please contact us at support@example.com", normalFont));

            document.close();
            writer.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF invoice", e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    // Method to generate sample PDF without needing an actual booking object
    public byte[] generateSampleInvoice() {
        // Create sample booking with dummy data
        Booking sampleBooking = createSampleBooking();

        // Use the existing method to generate PDF
        return generateBookingInvoice(sampleBooking);
    }

    // Helper method to create sample booking data
    private Booking createSampleBooking() {
        // Create sample rooms
        Room room1 = new Room();
        room1.setRoomType("Deluxe King");
        room1.setPrice(199.99);

        Room room2 = new Room();
        room2.setRoomType("Superior Suite");
        room2.setPrice(299.99);

        List<Room> rooms = new ArrayList<>(Arrays.asList(room1, room2));

        // Create sample booking
        Booking booking = new Booking();
        booking.setBookingReference("BK" + System.currentTimeMillis());
        booking.setCheckInDate(LocalDate.now().plusDays(30));
        booking.setCheckOutDate(LocalDate.now().plusDays(33));
        booking.setAdultsCount(2);
        booking.setChildrenCount(1);
        booking.setRooms(rooms);
        booking.setTotalAmount(199.99 * 3 + 299.99 * 3); // 3 nights
        booking.setGuestName("John Smith");
        booking.setGuestEmail("john.smith@example.com");
        booking.setPaymentMethod("Credit Card");
        booking.setTransactionId("TXN" + System.currentTimeMillis());
        booking.setTaxAmount(booking.getTotalAmount() * 0.1); // 10% tax

        return booking;
    }
}