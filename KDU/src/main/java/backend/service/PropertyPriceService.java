package backend.service;

import backend.constants.GraphQLQueries;
import backend.utils.CalculateMinPrice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.SortedMap;

@Service
public class PropertyPriceService {
    @Value("${graphql.endpoint}")
    private String graphqlEndpoint;

    @Value("${graphql.apiKey}")
    private String apiKey;

    private final CalculateMinPrice calculateMinimumRates;

    private final RestTemplate restTemplate = new RestTemplate();

    public PropertyPriceService(CalculateMinPrice calculateMinimumRates) {
        this.calculateMinimumRates = calculateMinimumRates;
    }

    public SortedMap<String, Double> fetchMinimumRoomRates(int propertyId)  {
        String query = GraphQLQueries.getFindPriceByDateQuery(propertyId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);  // Optional if authentication is needed

        HttpEntity<String> request = new HttpEntity<>(query, headers);
        ResponseEntity<String> response = restTemplate.exchange(graphqlEndpoint, HttpMethod.POST, request, String.class);

        return calculateMinimumRates.calculateMinimumRates(response.getBody());
    }
}
