package backend.service.impl;

import backend.constants.GraphQLQueries;
import backend.dto.response.PromotionResponseDTO;
import backend.service.PromotionService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${graphql.endpoint}")
    private String graphqlEndpoint;

    @Value("${graphql.api-key}")
    private String apiKey;

    @Override
    public List<PromotionResponseDTO> getAllPromotions() {
        try {
            JsonNode response = executeGraphQLQuery(GraphQLQueries.GET_PROMOTIONS_QUERY);
            return processGraphQLResponse(response);
        } catch (Exception e) {
            log.error("Failed to get promotions: {}", e.getMessage());
            throw new RuntimeException("Failed to get promotions", e);
        }
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

    private List<PromotionResponseDTO> processGraphQLResponse(JsonNode response) {
        List<PromotionResponseDTO> promotions = new ArrayList<>();
        
        try {
            JsonNode data = response.get("data");
            if (data != null && data.has("listPromotions")) {
                JsonNode promotionsNode = data.get("listPromotions");
                
                for (JsonNode promotionNode : promotionsNode) {
                    try {
                        PromotionResponseDTO promotion = objectMapper.treeToValue(promotionNode, PromotionResponseDTO.class);
                        if (promotion != null) {
                            promotions.add(promotion);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse promotion: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process promotions: {}", e.getMessage());
            throw new RuntimeException("Failed to process GraphQL response", e);
        }
        
        return promotions;
    }
} 