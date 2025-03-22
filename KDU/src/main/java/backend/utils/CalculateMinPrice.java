package backend.utils;

import backend.exception.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.SortedMap;
import java.util.TreeMap;

@Service
public class CalculateMinPrice {
    private final ObjectMapper objectMapper;
    public CalculateMinPrice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public SortedMap<String, Double> calculateMinimumRates(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode listPropertiesNode = rootNode.path("data").path("listProperties");

            SortedMap<String, Double> minimumRatesByDate = new TreeMap<>();

            for (JsonNode propertyNode : listPropertiesNode) {
                for (JsonNode roomTypeNode : propertyNode.path("room_type")) {
                    for (JsonNode roomRateNode : roomTypeNode.path("room_rates")) {
                        JsonNode roomRateData = roomRateNode.path("room_rate");
                        String date = roomRateData.path("date").asText();
                        double basicNightlyRate = roomRateData.path("basic_nightly_rate").asDouble();

                        // Always store the minimum value for each date
                        minimumRatesByDate.merge(date, basicNightlyRate, Math::min);
                    }
                }
            }

            return minimumRatesByDate;
        } catch (JsonProcessingException e) {
            throw new JsonParseException("Error parsing JSON response");
        }
    }


}
