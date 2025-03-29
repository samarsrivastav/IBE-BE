package backend.integration;

import backend.entity.CustomPromotion;
import backend.repository.CustomPromotionRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class CustomPromotionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CustomPromotionRepository repository;

    private CustomPromotion promotion;

    @BeforeEach
    @Transactional
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        repository.deleteAll();  // Ensure a clean state before each test

        promotion = new CustomPromotion();
        promotion.setTenantId(1L);
        promotion.setStartDate(LocalDate.now());
        promotion.setEndDate(LocalDate.now().plusDays(7));
        promotion.setDiscount(10);
        promotion.setDescription("LOREM IPSUM");
        promotion.setTitle("PROMOTION");
        repository.save(promotion);
    }

//    @Test
//    void createPromotion_ShouldReturnPromotion() throws Exception {
//        String json = """
//                {
//                    "tenantId": 1,
//                    "startDate": "2025-01-01",
//                    "endDate": "2025-01-07",
//                    "discount": "15%"
//                }
//                """;
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/custom-promotions")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(json))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.discount").value("15%"));
//    }

    @Test
    void getPromotionsByTenant_ShouldReturnList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/custom-promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deletePromotion_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/custom-promotions/1/" + promotion.getPromotionId()))
                .andExpect(status().isNoContent());
    }

//    @Test
//    void createPromotion_InvalidTenant_ShouldReturnForbidden() throws Exception {
//        String json = """
//                {
//                    "tenantId": 2,
//                    "startDate": "2025-01-01",
//                    "endDate": "2025-01-07",
//                    "discount": "20%"
//                }
//                """;
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/custom-promotions")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(json))
//                .andExpect(status().isForbidden()); // Tenant validation should fail
//    }
}
