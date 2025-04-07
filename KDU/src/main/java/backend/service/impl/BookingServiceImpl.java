package backend.service.impl;


import backend.constants.BookingRoomAvailabilityQueries;
import backend.dto.request.BookingRequestDto;

import backend.dto.request.ConfirmationDetailsDto;
import backend.exception.RoomNotAvailableException;
import backend.model.*;
import backend.repository.BookingTransactionRepository;
import backend.repository.PseudoBookingRepository;
import backend.repository.SpecialOffersRepository;
import backend.service.BookingService;
import backend.service.ConfirmationBookingEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final RestTemplate restTemplate;
    private final PseudoBookingRepository pseudoBookingRepository;
    private final BookingTransactionRepository bookingTransactionRepository;
    private final SpecialOffersRepository specialOffersRepository;
    private final ConfirmationBookingEmailService confirmationBookingEmailService;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Value("${graphql.api.key}")
    private String apiKey;

    @Value("${graphql.url}")
    private String graphqlEndpoint;

    // Counter for booking group IDs
    private static final AtomicLong bookingGroupIdCounter = new AtomicLong(1);

    @Override
    public UUID checkIfBookingIsPossible(long propertyId, String startDate, String endDate, long roomTypeId, long roomCount, BookingRequestDto bookingRequestDto) throws RoomNotAvailableException, JsonProcessingException {
        log.info("=== Starting Booking Process ===");
        log.info("Checking room availability for property: {}, room type: {}, dates: {} to {}", 
                propertyId, roomTypeId, startDate, endDate);
        
        try {
            // Step 1: Check room availability
            log.info("Step 1: Finding available rooms");
            Map<Integer, IndividualRoom> availableRoomIds = findOutRoomAvailability(propertyId, startDate, endDate, roomTypeId, roomCount);
            
            if (availableRoomIds.isEmpty()) {
                log.warn("No rooms available for the requested criteria");
                throw new RoomNotAvailableException("There are no rooms available. Try again after sometime");
            }
            
            log.info("Found {} available rooms", availableRoomIds.size());

            // Step 2: Process dates
            log.info("Step 2: Processing booking dates");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDateFormat = LocalDate.parse(startDate, formatter);
            LocalDate endDateFormat = LocalDate.parse(endDate, formatter);
            log.info("Booking period: {} to {} ({} nights)", 
                    startDateFormat, endDateFormat, 
                    ChronoUnit.DAYS.between(startDateFormat, endDateFormat));

            // Step 3: Create booking transaction
            log.info("Step 3: Creating booking transaction");
            BookingTransaction bookingTransaction = bookRoomsWithDailyEntries(
                availableRoomIds,
                startDateFormat,
                endDateFormat,
                roomCount,
                bookingRequestDto.getConfirmationDetails(),
                bookingRequestDto.getBillingInfo().getFirstName(),
                bookingRequestDto.getConfirmationDetails().getPromotionTitle()
            );
            System.out.println("Booking transaction ID: " + bookingTransaction.getId());
            log.info("Booking transaction created with ID: {}", bookingTransaction.getId());

            // Step 4: Save booking details
            log.info("Step 4: Saving booking details");
            JsonNode bookingDetails = convertBookingDtoToJsonNode(bookingRequestDto);
            bookingTransaction.setBookingDetails(bookingDetails);
            bookingTransaction.setEmail(bookingRequestDto.getBillingInfo().getEmail());
            bookingTransactionRepository.save(bookingTransaction);
            bookingTransaction.setHasReviewed(false);
            log.info("Booking details saved successfully");

            // Step 5: Handle special offers
            if (bookingRequestDto.isSpecialOffers()) {
                log.info("Step 5: Processing special offers");
                SpecialOffers specialOffers = new SpecialOffers();
                specialOffers.setEmail(bookingRequestDto.getBillingInfo().getEmail());
                specialOffersRepository.save(specialOffers);
                log.info("Special offers saved for email: {}", specialOffers.getEmail());
            }

            // Step 6: Send confirmation email
            log.info("Step 6: Sending confirmation email");
            executorService.submit(() -> {
                log.info("Sending confirmation email to: {}", bookingRequestDto.getBillingInfo().getEmail());
                confirmationBookingEmailService.emailConfirmationDetailsOfBooking(
                    bookingTransaction.getConfirmationId(),
                    bookingRequestDto.getBillingInfo().getEmail()
                );
            });

            log.info("=== Booking Process Completed Successfully ===");
            return bookingTransaction.getConfirmationId();
        } catch (Exception e) {
            log.error("Error in booking process: {}", e.getMessage(), e);
            throw e;
        }

    }

    private JsonNode convertBookingDtoToJsonNode(BookingRequestDto bookingRequestDto) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.valueToTree(bookingRequestDto);
    }

    private Map<Integer, IndividualRoom> findOutRoomAvailability(
            long propertyId,
            String startDateString,
            String endDateString,
            long roomTypeId,
            long roomCount
    ) throws JsonProcessingException, RoomNotAvailableException {

        // Format the date range into AWSDateTime standard format
        String startDate = startDateString + "T00:00:00.000Z";
        String endDate = endDateString + "T00:00:00.000Z";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        LocalDateTime startDateFormat = LocalDateTime.parse(startDate, formatter);
        LocalDateTime endDateFormat = LocalDateTime.parse(endDate, formatter);


        long daysBetween = ChronoUnit.DAYS.between(startDateFormat, endDateFormat) + 1;


        // Build the query
        String availableRoomsQuery = BookingRoomAvailabilityQueries.getBookingsRoomAvailableQuery(
                propertyId, startDate, endDate, roomTypeId);

        log.debug("Room availability query: {}", availableRoomsQuery);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey); // Auth header

        // Wrap the query inside JSON body
        Map<String, String> requestBody = Map.of("query", availableRoomsQuery);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Make the request
            JsonNode response = restTemplate.postForObject(graphqlEndpoint, request, JsonNode.class);

            if (response == null) {
                throw new RuntimeException("Empty response from GraphQL endpoint");
            }

            if (response.has("errors")) {
                String errorMessage = response.get("errors").get(0).get("message").asText();
                throw new RuntimeException("GraphQL error: " + errorMessage);
            }
            // Parse and map result
            return mapRoomAvailability(response.toString(), daysBetween, roomCount);

        } catch (Exception e) {
            log.error("Error fetching room availability: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch room availability", e);
        }
    }


    private Map<Integer, IndividualRoom> mapRoomAvailability(String jsonResponse, long daysBetween, long roomCount) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Integer, IndividualRoom> roomsForBooking = new HashMap<>();
        
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        log.debug("Room availability response: {}", jsonResponse);
        
        // Check if the response has the expected structure
        if (rootNode == null || !rootNode.has("data") || !rootNode.get("data").has("listRoomAvailabilities")) {
            log.error("Invalid response structure from GraphQL: {}", jsonResponse);

            return roomsForBooking;
        }
        
        JsonNode availableBookings = rootNode.get("data").get("listRoomAvailabilities");


        if (!availableBookings.isArray()) {
            log.error("listRoomAvailabilities is not an array: {}", availableBookings);
            return roomsForBooking;
        }
        
        for (JsonNode node : availableBookings) {
            if (!node.has("room_id") || !node.has("availability_id")) {
                log.warn("Skipping invalid room availability node: {}", node);
                continue;
            }
            
            int roomId = node.get("room_id").asInt();
            int availabilityId = node.get("availability_id").asInt();
            
            IndividualRoom individualRoom = roomsForBooking.getOrDefault(roomId, new IndividualRoom());
            List<Integer> availabilityIds = individualRoom.getAvailabilityId();
            if (availabilityIds == null) {
                availabilityIds = new ArrayList<>();
                individualRoom.setAvailabilityId(availabilityIds);
            }
            availabilityIds.add(availabilityId);
            individualRoom.setAvailabilityId(availabilityIds);

            roomsForBooking.put(roomId, individualRoom);
        }
        
        // Filter out rooms that don't have availability for the entire stay
        Iterator<Map.Entry<Integer, IndividualRoom>> iterator = roomsForBooking.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, IndividualRoom> entry = iterator.next();
            if (entry.getValue().getAvailabilityId() == null || 
                entry.getValue().getAvailabilityId().size() < daysBetween) {
                iterator.remove();
            }
        }
        
        // If we don't have enough rooms, return empty map
        if (roomsForBooking.size() < roomCount) {
            log.info("Not enough available rooms. Found: {}, Required: {}", roomsForBooking.size(), roomCount);
            return new HashMap<>();
        }
        return roomsForBooking;
    }


    /**
     * Book rooms with daily entries for better concurrency control
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    protected BookingTransaction bookRoomsWithDailyEntries(
            Map<Integer, IndividualRoom> availableRooms,
            LocalDate startDate,
            LocalDate endDate,
            long roomCount,
            ConfirmationDetailsDto confirmationDetailsDto,
            String guestName,
            String PromoTitle) throws RoomNotAvailableException {

        // Generate a unique booking group ID
        long bookingGroupId = bookingGroupIdCounter.getAndIncrement();
        log.info("Starting booking process with group ID: {}", bookingGroupId);

        // Track successful room bookings
        Map<Integer, IndividualRoom> successfulBookings = new HashMap<>();
        double val = Math.random() * 100;
        long bookedRoomCount = 0;

        try {
            // For each potentially available room
            roomLoop:
            for (Map.Entry<Integer, IndividualRoom> entry : availableRooms.entrySet()) {
                if (bookedRoomCount >= roomCount) {
                    break;
                }

                Integer roomId = entry.getKey();
                log.info("Attempting to book room ID: {}", roomId);

                // First check if room is already booked for any day in the range
                int existingBookingsCount = pseudoBookingRepository.countBookingsForRoomInDateRange(
                        roomId, startDate, endDate);

                if (existingBookingsCount > 0) {
                    log.info("Room {} already has bookings in this period", roomId);
                    continue;
                }

                // Try to create a booking entry for each day
                List<PseudoBooking> dailyBookings = new ArrayList<>();
                LocalDate currentDate = startDate;

                while (!currentDate.isAfter(endDate)) {
                    PseudoBookingId id = new PseudoBookingId(roomId, currentDate);
                    PseudoBooking booking = new PseudoBooking();
                    booking.setPseudoBookingId(id);
                    booking.setBookingGroupId(bookingGroupId);
                    booking.setVersion((int) val);

                    try {
                        pseudoBookingRepository.save(booking);
                        dailyBookings.add(booking);
                        log.info("Created pseudo-booking for room {} on date {}", roomId, currentDate);
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Room {} already booked for date {}", roomId, currentDate);
                        for (PseudoBooking savedBooking : dailyBookings) {
                            pseudoBookingRepository.delete(savedBooking);
                        }
                        continue roomLoop;
                    }

                    currentDate = currentDate.plusDays(1);
                }

                // If we got here, all days were successfully booked
                successfulBookings.put(roomId, entry.getValue());
                bookedRoomCount++;
                log.info("Successfully booked room ID: {} ({} of {} rooms)", roomId, bookedRoomCount, roomCount);
            }

            // Check if we got enough rooms
            if (bookedRoomCount < roomCount) {
                log.warn("Could not book enough rooms. Booked: {}, Required: {}", bookedRoomCount, roomCount);
                cleanupPseudoBookings(bookingGroupId);
                throw new RoomNotAvailableException("Could not book the required number of rooms due to unavailability");
            }

            // Process the successful booking
            log.info("Attempting GraphQL mutation for booking group ID: {}", bookingGroupId);
            BookingTransaction transaction = mutateTheBookingTable(successfulBookings, confirmationDetailsDto, guestName, PromoTitle);
            log.info("GraphQL mutation successful. Transaction ID: {}", transaction.getId());

            // Clean up pseudo-bookings in a new transaction
            cleanupPseudoBookings(bookingGroupId);

            return transaction;

        } catch (Exception e) {
            log.error("Error during booking process", e);
            cleanupPseudoBookings(bookingGroupId);
            if (e instanceof RoomNotAvailableException) {
                throw (RoomNotAvailableException)e;
            }
            throw new RuntimeException("An error occurred while processing the booking", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void cleanupPseudoBookings(long bookingGroupId) {
        try {
            log.info("Cleaning up pseudo-bookings for group ID: {}", bookingGroupId);
            pseudoBookingRepository.deleteByBookingGroupId(bookingGroupId);
            log.info("Successfully cleaned up pseudo-bookings for group ID: {}", bookingGroupId);
        } catch (Exception e) {
            log.error("Error cleaning up pseudo-bookings for group ID: {}", bookingGroupId, e);
            // Don't throw the exception as the main booking is already successful
        }
    }

    private BookingTransaction mutateTheBookingTable(
            Map<Integer, IndividualRoom> bookedRoomsWithAvailabilityId,
            ConfirmationDetailsDto confirmationDetailsDto,
            String guestName,
            String promoTitle
    ) throws JsonProcessingException {

        // Build the mutation query

        String mutationQuery = BookingRoomAvailabilityQueries.getBookingsMutationQuery(confirmationDetailsDto, guestName);
        System.out.println("Mutation query: " + mutationQuery);

        log.debug("Booking mutation query: {}", mutationQuery);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey); // Auth header

        // Wrap the mutation query inside JSON body
        Map<String, String> requestBody = Map.of("query", mutationQuery);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        System.out.println("Request body: " + requestBody);

        try {
            // Make the request
            String response = restTemplate.postForObject(graphqlEndpoint, request, String.class);
            System.out.println("Response: " + response);

            if (response == null) {
                throw new RuntimeException("Empty response from GraphQL endpoint");
            }

            // Parse the response and extract booking ID
            long bookingId = parseMutatedBooking(response);
            System.out.println("Booking ID: " + bookingId);

            // Update availability IDs in the database
            System.out.println("Updating availability IDs in the database");
            List<Integer> availabilityIds = updateAvailabilityIdInGraphQlDatabase(bookedRoomsWithAvailabilityId, bookingId);

            // Build and return the transaction object
            BookingTransaction bookingTransaction = new BookingTransaction();
            bookingTransaction.setId(bookingId);
            bookingTransaction.setAvailabilityId(availabilityIds);
            bookingTransaction.setActive(true);
            return bookingTransaction;

        } catch (Exception e) {
            log.error("Error mutating booking table: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mutate booking table", e);
        }
    }


    public long parseMutatedBooking(String jsonResponse) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        return jsonNode.get("data").get("createBooking").get("booking_id").asLong();
    }

    public List<Integer> updateAvailabilityIdInGraphQlDatabase(
            Map<Integer, IndividualRoom> bookedAvailableId,
            long bookingId
    ) {
        List<Integer> availabilityIds = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        for (Map.Entry<Integer, IndividualRoom> entry : bookedAvailableId.entrySet()) {
            IndividualRoom room = entry.getValue();

            for (Integer availabilityId : room.getAvailabilityId()) {
                // Get the update query
                String updateQuery = BookingRoomAvailabilityQueries.queryOfUpdatingRoomAvailability(
                        availabilityId, bookingId
                );

                // Wrap the query in a JSON object with a "query" field
                Map<String, String> requestBody = Map.of("query", updateQuery);

                // Create request entity with JSON body
                HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

                System.out.println("Request body: " + requestBody);

                // Make the request
                ResponseEntity<String> response = restTemplate.exchange(
                        graphqlEndpoint, HttpMethod.POST, request, String.class
                );

                System.out.println("Response: " + response);

                availabilityIds.add(availabilityId);
            }
        }

        return availabilityIds;
    }
}