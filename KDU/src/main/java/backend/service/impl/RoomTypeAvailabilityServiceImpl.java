package backend.service.impl;

import backend.constants.GraphQLQueries;
import backend.dto.request.RoomTypeSearchRequestDTO;
import backend.dto.response.RoomTypeResponseDTO;
import backend.entity.RoomTypes;
import backend.service.RoomTypeAvailabilityService;
import backend.service.RoomTypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTypeAvailabilityServiceImpl implements RoomTypeAvailabilityService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RoomTypeService roomTypeService;

    @Value("${graphql.endpoint}")
    private String graphqlEndpoint;

    @Value("${graphql.api-key}")
    private String apiKey;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00.000'Z'");
    private static final int DEFAULT_PAGE_SIZE = 10;

    Map<Long, Double> averageRates = new HashMap<>();

    @Override
    public List<RoomTypeResponseDTO> getAvailableRoomTypes(RoomTypeSearchRequestDTO searchRequest) {
        try {
            String formattedStartDate = searchRequest.getStartDate().format(DATE_FORMATTER);
            String formattedEndDate = searchRequest.getEndDate().format(DATE_FORMATTER);

            // Get available room types
            String query = GraphQLQueries.getRoomTypeByDate(
                searchRequest.getPropertyId(),
                formattedStartDate,
                formattedEndDate,
                searchRequest.getNumberOfGuests()
            );

            JsonNode response = executeGraphQLQuery(query);
            List<RoomTypeResponseDTO> graphQLRoomTypes = processGraphQLResponse(response);

            if (searchRequest.getTotalBeds() != null) {
                graphQLRoomTypes = filterByBedCount(graphQLRoomTypes, searchRequest);
            }

            if (searchRequest.getNumberOfRooms() != null) {
                graphQLRoomTypes = filterByRoomCount(graphQLRoomTypes, searchRequest);
            }

            // Get room rates for available room types
            Set<Long> roomTypeIds = graphQLRoomTypes.stream()
                .map(RoomTypeResponseDTO::getId)
                .collect(Collectors.toSet());
            
             averageRates = getAverageRoomRates(roomTypeIds, formattedStartDate, formattedEndDate);

            // Get local room types data
            List<RoomTypes> localRoomTypes = roomTypeService.getAllRoomTypes();
            List<RoomTypes> matchingLocalRoomTypes = localRoomTypes.stream()
                .filter(roomType -> roomTypeIds.contains(roomType.getRoomTypeId()))
                .collect(Collectors.toList());

            // Merge all data
            List<RoomTypeResponseDTO> result = mergeRoomTypeData(graphQLRoomTypes, matchingLocalRoomTypes);
            
            // Set average rates
            result.forEach(roomType -> {
                Double rate = averageRates.get(roomType.getId());
                roomType.setPrice(rate);
                log.info("Setting average rate {} for room type {}", rate, roomType.getId());
            });

            // Apply pagination
            return applyPagination(result, searchRequest);

        } catch (Exception e) {
            log.error("Failed to get available room types: {}", e.getMessage());
            throw new RuntimeException("Failed to get available room types", e);
        }
    }

    private List<RoomTypeResponseDTO> applyPagination(List<RoomTypeResponseDTO> roomTypes, RoomTypeSearchRequestDTO searchRequest) {
        int pageSize = searchRequest.getPageSize() != null ? searchRequest.getPageSize() : DEFAULT_PAGE_SIZE;
        String cursor = searchRequest.getCursor();

        if (cursor == null) {
            // First page
            return roomTypes.stream()
                .limit(pageSize)
                .collect(Collectors.toList());
        }

        // Find the starting index based on the cursor
        int startIndex = findStartIndex(roomTypes, cursor);
        if (startIndex == -1) {
            return Collections.emptyList();
        }

        // Return the next page of results
        return roomTypes.stream()
            .skip(startIndex)
            .limit(pageSize)
            .collect(Collectors.toList());
    }

    private int findStartIndex(List<RoomTypeResponseDTO> roomTypes, String cursor) {
        try {
            Long cursorId = Long.parseLong(cursor);
            for (int i = 0; i < roomTypes.size(); i++) {
                if (roomTypes.get(i).getId().equals(cursorId)) {
                    return i + 1; // Start from the next item
                }
            }
        } catch (NumberFormatException e) {
            log.error("Invalid cursor format: {}", cursor);
        }
        return -1;
    }

    private Map<Long, Double> getAverageRoomRates(Set<Long> roomTypeIds, String startDate, String endDate) {
        try {
            String roomTypeIdsString = roomTypeIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
            log.info("Fetching room rates for room types: {} between {} and {}", roomTypeIdsString, startDate, endDate);
            
            String query = GraphQLQueries.getRoomRatesByRoomTypes(roomTypeIdsString, startDate, endDate);
            JsonNode response = executeGraphQLQuery(query);
            Map<Long, List<Double>> ratesByRoomType = new HashMap<>();
            
            if (response.has("data") && response.get("data").has("listRoomRateRoomTypeMappings")) {
                JsonNode mappings = response.get("data").get("listRoomRateRoomTypeMappings");
                log.info("Found {} rate mappings", mappings.size());
                
                for (JsonNode mapping : mappings) {
                    Long roomTypeId = mapping.get("room_type_id").asLong();
                    JsonNode roomRate = mapping.get("room_rate");
                    if (roomRate != null && roomRate.has("basic_nightly_rate")) {
                        double rate = roomRate.get("basic_nightly_rate").asDouble();
                        ratesByRoomType.computeIfAbsent(roomTypeId, k -> new ArrayList<>()).add(rate);
                    }
                }
            }

            // Calculate averages
            Map<Long, Double> averageRates = new HashMap<>();
            ratesByRoomType.forEach((roomTypeId, rates) -> {
                if (!rates.isEmpty()) {
                    double average = rates.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                    averageRates.put(roomTypeId, average);
                    log.info("Room type {} has {} rates, average: {}", roomTypeId, rates.size(), average);
                } else {
                    log.warn("No rates found for room type {}", roomTypeId);
                }
            });

            // Log room types without rates
            roomTypeIds.forEach(roomTypeId -> {
                if (!averageRates.containsKey(roomTypeId)) {
                    log.warn("No average rate calculated for room type {}", roomTypeId);
                }
            });

            return averageRates;
        } catch (Exception e) {
            log.error("Failed to get average room rates: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private List<RoomTypeResponseDTO> mergeRoomTypeData(List<RoomTypeResponseDTO> graphQLRoomTypes, List<RoomTypes> localRoomTypes) {
        Map<Long, RoomTypes> localRoomTypeMap = localRoomTypes.stream()
            .collect(Collectors.toMap(RoomTypes::getRoomTypeId, roomType -> roomType));

        return graphQLRoomTypes.stream()
            .map(graphQLType -> {
                RoomTypes localType = localRoomTypeMap.get(graphQLType.getId());
                if (localType != null) {
                    graphQLType.setDescription(localType.getRoomTypeDescription());
                    graphQLType.setReviews(localType.getNumberOfReviews());
                    graphQLType.setRating(localType.getStars());
                    graphQLType.setImages(localType.getImages());
                    graphQLType.setAmenities(localType.getAmenities());
                    graphQLType.setLocation(localType.getLocation());
                }
                return graphQLType;
            })
            .collect(Collectors.toList());
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

    private List<RoomTypeResponseDTO> processGraphQLResponse(JsonNode response) {
        List<RoomTypeResponseDTO> roomTypes = new ArrayList<>();
        
        try {
            JsonNode data = response.get("data");
            if (data != null && data.has("listRoomAvailabilities")) {
                JsonNode availabilities = data.get("listRoomAvailabilities");
                
                Map<Long, Integer> roomTypeAvailabilityCount = new HashMap<>();
                
                for (JsonNode availability : availabilities) {
                    if (availability.has("room") && availability.get("room").has("room_type")) {
                        JsonNode roomTypeNode = availability.get("room").get("room_type");
                        Long roomTypeId = roomTypeNode.get("room_type_id").asLong();
                        roomTypeAvailabilityCount.merge(roomTypeId, 1, Integer::sum);
                    }
                }
                
                for (JsonNode availability : availabilities) {
                    if (availability.has("room") && availability.get("room").has("room_type")) {
                        JsonNode roomTypeNode = availability.get("room").get("room_type");
                        try {
                            RoomTypeResponseDTO roomType = objectMapper.treeToValue(roomTypeNode, RoomTypeResponseDTO.class);
                            if (roomType != null) {
                                if (roomType.getSingleBed() == null) roomType.setSingleBed(0);
                                if (roomType.getDoubleBed() == null) roomType.setDoubleBed(0);
                                if (roomType.getMaxOccupancy() == null) roomType.setMaxOccupancy(0);
                                
                                roomType.setAvailableRooms(roomTypeAvailabilityCount.get(roomType.getId()));
                                roomTypes.add(roomType);
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse room type: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process GraphQL response: {}", e.getMessage());
            throw new RuntimeException("Failed to process GraphQL response", e);
        }
        
        return roomTypes;
    }

    private List<RoomTypeResponseDTO> filterByBedCount(List<RoomTypeResponseDTO> roomTypes, RoomTypeSearchRequestDTO searchRequest) {
        return roomTypes.stream()
            .filter(roomType -> {
                int singleBeds = roomType.getSingleBed() != null ? roomType.getSingleBed() : 0;
                int doubleBeds = roomType.getDoubleBed() != null ? roomType.getDoubleBed() : 0;
                int totalBedsInRoom = singleBeds + (doubleBeds * 2);
                return totalBedsInRoom >= searchRequest.getTotalBeds();
            })
            .collect(Collectors.toList());
    }

    private List<RoomTypeResponseDTO> filterByRoomCount(List<RoomTypeResponseDTO> roomTypes, RoomTypeSearchRequestDTO searchRequest) {
        Map<Long, Long> roomTypeCounts = roomTypes.stream()
            .collect(Collectors.groupingBy(
                RoomTypeResponseDTO::getId,
                Collectors.counting()
            ));

        return roomTypes.stream()
            .filter(roomType -> roomTypeCounts.get(roomType.getId()) >= searchRequest.getNumberOfRooms())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public double getRoomTypeRate(Long roomTypeId) {
       try {
         if (averageRates.containsKey(roomTypeId)) {
            return averageRates.get(roomTypeId);
         }
         return 0.0;
       } catch (Exception e) {
        log.error("Failed to get room type rate: {}", e.getMessage());
        return 0.0;
       }
    }

    @Override
    public Map<Long, Double> getAverageRates() {
        return averageRates;
    }

} 