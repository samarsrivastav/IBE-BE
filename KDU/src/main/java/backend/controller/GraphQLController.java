package backend.controller;

import backend.service.GraphQLService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/graphql")
public class GraphQLController {

    private final GraphQLService graphQLService;

    public GraphQLController(GraphQLService graphQLService) {
        this.graphQLService = graphQLService;
    }

    @GetMapping("/property")
    public Object getPropertyByName(@RequestParam String name) {
        return graphQLService.fetchPropertyByName(name);
    }
}

