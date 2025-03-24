package backend.utils;

import backend.exception.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.*;

class CalculateMinPriceTest {

    private CalculateMinPrice calculateMinPrice;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        calculateMinPrice = new CalculateMinPrice(objectMapper);
    }

    @Test
    void testCalculateMinimumRates() {
        String jsonResponse = "{ \"data\": { \"listProperties\": [ { \"room_type\": [ { \"room_rates\": [ { \"room_rate\": { \"date\": \"2023-01-01\", \"basic_nightly_rate\": 100.0 } } ] } ] } ] } }";
        SortedMap<String, Double> result = calculateMinPrice.calculateMinimumRates(jsonResponse);
        assertEquals(1, result.size());
        assertEquals(100.0, result.get("2023-01-01"));
    }

    @Test
    void testCalculateMinimumRatesWithInvalidJson() {
        String invalidJsonResponse = "{ \"data\": { \"listProperties\": [ { \"room_type\": [ { \"room_rates\": [ { \"room_rate\": { \"date\": \"2023-01-01\", \"basic_nightly_rate\": 100.0 } } ] } ] } ] }";
        assertThrows(JsonParseException.class, () -> calculateMinPrice.calculateMinimumRates(invalidJsonResponse));
    }
}