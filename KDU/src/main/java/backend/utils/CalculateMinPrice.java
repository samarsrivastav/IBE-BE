package backend.utils;

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
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        JsonNode listPropertiesNode = rootNode.path("data").path("listProperties");

        SortedMap<String, Double> minimumRatesByDate = new TreeMap<>();

        for (JsonNode propertyNode : listPropertiesNode) {
            JsonNode roomTypeNode = propertyNode.path("room_type");
            for (JsonNode roomRateNode : roomTypeNode) {
                JsonNode roomRatesNode = roomRateNode.path("room_rates");
                for (JsonNode roomRate : roomRatesNode) {
                    JsonNode roomRateData = roomRate.path("room_rate");
                    String date = roomRateData.path("date").asText();
                    double basicNightlyRate = roomRateData.path("basic_nightly_rate").asDouble();

                    minimumRatesByDate.computeIfPresent(date, (key, existingRate) -> Math.min(existingRate, basicNightlyRate));
                    minimumRatesByDate.putIfAbsent(date, basicNightlyRate);
                }
            }
        }
        return minimumRatesByDate;
    }
}
