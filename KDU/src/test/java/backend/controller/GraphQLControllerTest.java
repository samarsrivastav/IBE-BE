package backend.controller;

import backend.service.GraphQLService;
import backend.service.PropertyPriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GraphQLControllerTest {

    @Mock
    private GraphQLService graphQLService;

    @Mock
    private PropertyPriceService propertyPriceService;

    @InjectMocks
    private GraphQLController graphQLController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPropertyByName_ValidName_ShouldReturnProperty() {
        String propertyName = "Sample Property";
        Object expectedResponse = Map.of("name", "Sample Property", "location", "New York");

        when(graphQLService.fetchPropertyByName(propertyName)).thenReturn(expectedResponse);

        Object response = graphQLController.getPropertyByName(propertyName);

        assertEquals(expectedResponse, response);
        verify(graphQLService, times(1)).fetchPropertyByName(propertyName);
    }

    @Test
    void getMinimumRates_ValidPropertyId_ShouldReturnRates() {
        int propertyId = 101;
        SortedMap<String, Double> expectedRates = new TreeMap<>();
        expectedRates.put("Standard Room", 99.99);
        expectedRates.put("Deluxe Room", 149.99);

        when(propertyPriceService.fetchMinimumRoomRates(propertyId)).thenReturn(expectedRates);

        ResponseEntity<Map<String, Double>> response = graphQLController.getMinimumRates(propertyId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedRates, response.getBody());
        verify(propertyPriceService, times(1)).fetchMinimumRoomRates(propertyId);
    }
}
