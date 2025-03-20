package backend.controller;

import backend.service.GraphQLService;
import backend.service.PropertyPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.SortedMap;

@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class GraphQLController {

    private final GraphQLService graphQLService;
    private final PropertyPriceService propertyPriceService;

    @GetMapping("/property")
    public Object getPropertyByName(@RequestParam String name) {
        return graphQLService.fetchPropertyByName(name);
    }
    @GetMapping("/property-rate")
    public ResponseEntity<Map<String, Double>> getMinimumRates() {
        SortedMap<String, Double> minimumRates = propertyPriceService.fetchMinimumRoomRates();
        return ResponseEntity.ok(minimumRates);
    }
}

