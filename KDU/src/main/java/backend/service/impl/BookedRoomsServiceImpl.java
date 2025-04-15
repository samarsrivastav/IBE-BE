package backend.service.impl;

import backend.constants.HouseKeeping;
import backend.dto.response.RoomAvailabilityResponseDTO;
import backend.model.BookingTransaction;
import backend.repository.BookingTransactionRepository;
import backend.service.BookedRoomsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookedRoomsServiceImpl implements BookedRoomsService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final BookingTransactionRepository bookingTransactionRepository;

    @Value("${graphql.endpoint}")
    private String graphqlEndpoint;

    @Value("${graphql.api-key}")
    private String apiKey;

    @Autowired
    public BookedRoomsServiceImpl(RestTemplate restTemplate, BookingTransactionRepository bookingTransactionRepository) {
        this.restTemplate = restTemplate;
        this.bookingTransactionRepository = bookingTransactionRepository;
    }

    @Override
    public List<Long> getCheckInRoomsForToday(int propertyId) {
        String testDate = "2025-06-21";
        log.info("Checking check-in rooms for date: {}", testDate);
        
        List<BookingTransaction> transactions = bookingTransactionRepository.findByCheckInDate(testDate);
        log.info("Found {} transactions with check-in date {}", transactions.size(), testDate);
        
        List<Long> bookingIds = transactions.stream()
            .map(BookingTransaction::getId)
            .collect(Collectors.toList());
        log.info("Booking IDs for check-in: {}", bookingIds);
        
        return getRoomIdsForBookings(propertyId, bookingIds, testDate);
    }

    @Override
    public List<Long> getCheckOutRoomsForToday(int propertyId) {
        String testDate = "2025-06-21";
        log.info("Checking check-out rooms for date: {}", testDate);
        
        List<BookingTransaction> transactions = bookingTransactionRepository.findByCheckOutDate(testDate);
        log.info("Found {} transactions with check-out date {}", transactions.size(), testDate);
        
        List<Long> bookingIds = transactions.stream()
            .map(BookingTransaction::getId)
            .collect(Collectors.toList());
        log.info("Booking IDs for check-out: {}", bookingIds);

        LocalDate date = LocalDate.parse(testDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        date = date.minusDays(1);
        testDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return getRoomIdsForBookings(propertyId, bookingIds, testDate);
    }

    @Override
    public List<Long> getCurrentlyBookedRooms(int propertyId) {
        String testDate = "2025-06-21T00:00:00Z";
        log.info("Checking currently booked rooms for date: {}", testDate);
        
        List<Integer> roomTypeIds = getRoomTypeIdsForProperty(propertyId);
        String query = HouseKeeping.getBookedRoomsInDateRangeQuery(propertyId, testDate, roomTypeIds);
        JsonNode response = executeGraphQLQuery(query);
        
        List<RoomAvailabilityResponseDTO> availabilities = processGraphQLResponse(response);
        List<Long> roomIds = new ArrayList<>();
        
        for (RoomAvailabilityResponseDTO availability : availabilities) {
            roomIds.add(availability.getRoomId());
        }
        
        log.info("Currently booked room IDs: {}", roomIds);
        return roomIds;
    }

    private List<Long> getRoomIdsForBookings(int propertyId, List<Long> bookingIds, String testDate) {
        if (bookingIds.isEmpty()) {
            log.info("No booking IDs found, returning empty list");
            return new ArrayList<>();
        }

        testDate = testDate + "T00:00:00Z";

        List<Integer> roomTypeIds = getRoomTypeIdsForProperty(propertyId);
        
        String query = HouseKeeping.getBookedRoomsInDateRangeQuery(propertyId, testDate, roomTypeIds);
        JsonNode response = executeGraphQLQuery(query);
        
        List<RoomAvailabilityResponseDTO> availabilities = processGraphQLResponse(response);
        List<Long> roomIds = availabilities.stream()
            .filter(availability -> bookingIds.contains(availability.getBookingId()))
            .map(RoomAvailabilityResponseDTO::getRoomId)
            .collect(Collectors.toList());
        
        log.info("Room IDs for bookings {}: {}", bookingIds, roomIds);
        return roomIds;
    }


    private List<Integer> getRoomTypeIdsForProperty(int propertyId) {
        if (propertyId == 8) {
            return List.of(43, 44, 45, 46, 47, 48);
        } else if (propertyId == 20) {
            return List.of(115, 116, 117, 118, 119, 120);
        }
        return new ArrayList<>();
    }

    private JsonNode executeGraphQLQuery(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        
        Map<String, String> requestBody = Map.of("query", query);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            JsonNode response = restTemplate.postForObject(
                graphqlEndpoint,
                request,
                JsonNode.class
            );
            
            if (response == null) {
                throw new RuntimeException("Empty response from GraphQL endpoint");
            }
            
            if (response.has("errors")) {
                String errorMessage = response.get("errors").get(0).get("message").asText();
                throw new RuntimeException("GraphQL error: " + errorMessage);
            }
            
            return response;
        } catch (Exception e) {
            log.error("GraphQL query failed: {}", e.getMessage());
            throw new RuntimeException("Failed to execute GraphQL query", e);
        }
    }

    private List<RoomAvailabilityResponseDTO> processGraphQLResponse(JsonNode response) {
        List<RoomAvailabilityResponseDTO> availabilities = new ArrayList<>();
        
        try {
            JsonNode data = response.get("data");
            if (data != null && data.has("listRoomAvailabilities")) {
                JsonNode availabilitiesNode = data.get("listRoomAvailabilities");
                
                for (JsonNode availabilityNode : availabilitiesNode) {
                    try {
                        RoomAvailabilityResponseDTO availability = objectMapper.treeToValue(availabilityNode, RoomAvailabilityResponseDTO.class);
                        if (availability != null) {
                            availabilities.add(availability);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse room availability: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process room availabilities: {}", e.getMessage());
            throw new RuntimeException("Failed to process GraphQL response", e);
        }
        
        return availabilities;
    }
}
