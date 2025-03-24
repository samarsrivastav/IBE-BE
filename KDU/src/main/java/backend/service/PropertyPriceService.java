package backend.service;

import backend.constants.GraphQLQueries;
import backend.utils.CalculateMinPrice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;

@Service
public class PropertyPriceService {

    private static final Logger logger = LoggerFactory.getLogger(PropertyPriceService.class);

    @Value("${graphql.endpoint}")
    private String graphqlEndpoint;

    @Value("${graphql.apiKey}")
    private String apiKey;

    private final CalculateMinPrice calculateMinimumRates;
    private final RestTemplate restTemplate = new RestTemplate();

    public PropertyPriceService(CalculateMinPrice calculateMinimumRates) {
        this.calculateMinimumRates = calculateMinimumRates;
    }

    public SortedMap<String, Double> fetchMinimumRoomRates(int propertyId) {
        logger.info("Fetching minimum room rates for propertyId: {}", propertyId);

        String query = GraphQLQueries.getFindPriceByDateQuery(propertyId);
        logger.debug("Generated GraphQL query: {}", query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);

        HttpEntity<String> request = new HttpEntity<>(query, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(graphqlEndpoint, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully retrieved response from GraphQL API for propertyId: {}", propertyId);
                return calculateMinimumRates.calculateMinimumRates(response.getBody());
            } else {
                logger.warn("Received non-200 response: {} for propertyId: {}", response.getStatusCode(), propertyId);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching minimum room rates for propertyId: {} - {}", propertyId, e.getMessage(), e);
            return null;
        }
    }
}
