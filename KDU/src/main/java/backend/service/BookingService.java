package backend.service;

import backend.dto.request.BookingRequestDto;
import backend.exception.RoomNotAvailableException;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.UUID;

public interface BookingService {
    /**
     * Checks if a booking is possible and processes the booking if rooms are available
     * 
     * @param propertyId the ID of the property
     * @param startDate the start date of the booking in format yyyy-MM-dd
     * @param endDate the end date of the booking in format yyyy-MM-dd
     * @param roomTypeId the ID of the room type
     * @param roomCount the number of rooms to book
     * @param bookingRequestDto the booking request data
     * @return the confirmation ID as UUID
     * @throws RoomNotAvailableException if rooms are not available
     * @throws JsonProcessingException if there is an error processing JSON data
     */
    UUID checkIfBookingIsPossible(long propertyId, String startDate, String endDate, long roomTypeId, long roomCount, BookingRequestDto bookingRequestDto) throws RoomNotAvailableException, JsonProcessingException;
} 