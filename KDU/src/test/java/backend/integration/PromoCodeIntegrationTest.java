package backend.integration;

import backend.dto.request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.entity.Enum.PromoType;
import backend.entity.PromoCode;
import backend.repository.PromoCodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PromoCodeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PromoCode activeOneTimePromo;
    private PromoCode activeTimeBasedPromo;
    private PromoCode inactivePromo;
    private PromoCode expiredPromo;
    private PromoCode futurePromo;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for LocalDate serialization
        objectMapper.registerModule(new JavaTimeModule());

        // Clear database before each test
        promoCodeRepository.deleteAll();

        // Create test promo codes
        LocalDate today = LocalDate.now();

        // Active ONE_TIME_USE promo
        activeOneTimePromo = PromoCode.builder()
                .name("ONE50")
                .title("50% Off One Time")
                .description("50% discount for one-time use")
                .discount(50.0)
                .isActive(true)
                .promoType(PromoType.ONE_TIME_USE)
                .startDate(today.minusDays(10))
                .endDate(today.plusDays(10))
                .build();

        // Active TIME_BASED promo
        activeTimeBasedPromo = PromoCode.builder()
                .name("TIME25")
                .title("25% Off Time Limited")
                .description("25% discount for limited time")
                .discount(25.0)
                .isActive(true)
                .promoType(PromoType.TIME_BASED)
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(5))
                .build();

        // Inactive promo
        inactivePromo = PromoCode.builder()
                .name("INACTIVE20")
                .title("Inactive 20% Off")
                .description("20% discount but inactive")
                .discount(20.0)
                .isActive(false)
                .promoType(PromoType.ONE_TIME_USE)
                .startDate(today.minusDays(10))
                .endDate(today.plusDays(10))
                .build();

        // Expired promo
        expiredPromo = PromoCode.builder()
                .name("EXPIRED30")
                .title("Expired 30% Off")
                .description("30% discount but expired")
                .discount(30.0)
                .isActive(true)
                .promoType(PromoType.TIME_BASED)
                .startDate(today.minusDays(30))
                .endDate(today.minusDays(1))
                .build();

        // Future promo - use a fixed date
        futurePromo = PromoCode.builder()
                .name("FUTURE40")
                .title("Future 40% Off")
                .description("40% discount starting in future")
                .discount(40.0)
                .isActive(true)
                .promoType(PromoType.ONE_TIME_USE)
                .startDate(LocalDate.of(2025, 4, 1))
                .endDate(LocalDate.of(2025, 4, 30))
                .build();

        // Save all promos
        activeOneTimePromo = promoCodeRepository.save(activeOneTimePromo);
        activeTimeBasedPromo = promoCodeRepository.save(activeTimeBasedPromo);
        inactivePromo = promoCodeRepository.save(inactivePromo);
        expiredPromo = promoCodeRepository.save(expiredPromo);
        futurePromo = promoCodeRepository.save(futurePromo);
    }

    @AfterEach
    void tearDown() {
        promoCodeRepository.deleteAll();
    }

    @Test
    void validateActiveOneTimePromoCode_success() throws Exception {
        // Prepare request
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());
        request.setPromoType(PromoType.ONE_TIME_USE);

        // Perform validation request
        MvcResult result = mockMvc.perform(post("/api/v1/promotions/validate/{code}", activeOneTimePromo.getName())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and verify response
        PromoCodeResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), PromoCodeResponseDTO.class);

        assertEquals(activeOneTimePromo.getId(), response.getId());
        assertEquals(activeOneTimePromo.getName(), response.getName());
        assertEquals(activeOneTimePromo.getDiscount(), response.getDiscount());
        assertEquals(PromoType.ONE_TIME_USE, response.getPromoType());
    }

    @Test
    void validateActiveTimeBasedPromoCode_success() throws Exception {
        // Prepare request
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());
        request.setPromoType(PromoType.TIME_BASED);

        // Perform validation request
        mockMvc.perform(post("/api/v1/promotions/validate/{code}", activeTimeBasedPromo.getName())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeTimeBasedPromo.getId()))
                .andExpect(jsonPath("$.name").value(activeTimeBasedPromo.getName()))
                .andExpect(jsonPath("$.discount").value(activeTimeBasedPromo.getDiscount()))
                .andExpect(jsonPath("$.promoType").value(activeTimeBasedPromo.getPromoType().toString()));
    }

    @Test
    void validateWithoutPromoType_success() throws Exception {
        // Prepare request without promo type (should work based on implementation)
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());

        // Perform validation request
        mockMvc.perform(post("/api/v1/promotions/validate/{code}", activeOneTimePromo.getName())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(activeOneTimePromo.getName()));
    }

    // Updated failure tests that use assertThrows consistently

    @Test
    void validateWithWrongPromoType_fails() throws Exception {
        // Prepare request with incorrect promo type
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());
        request.setPromoType(PromoType.TIME_BASED); // Wrong type for ONE_TIME_USE promo

        assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/promotions/validate/{code}", activeOneTimePromo.getName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });
    }

    @Test
    void validateInactivePromoCode_fails() throws Exception {
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());

        assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/promotions/validate/{code}", inactivePromo.getName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });
    }

    @Test
    void validateExpiredPromoCode_fails() throws Exception {
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());

        assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/promotions/validate/{code}", expiredPromo.getName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });
    }

    @Test
    void validateFuturePromoCode_fails() throws Exception {
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());

        assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/promotions/validate/{code}", futurePromo.getName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });
    }

    @Test
    void validateNonExistentPromoCode_fails() throws Exception {
        PromoCodeValidationRequestDTO request = new PromoCodeValidationRequestDTO();
        request.setUsageDate(LocalDate.now());

        assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/promotions/validate/{code}", "NONEXISTENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });
    }

    @Test
    void togglePromoCodeStatus_success() throws Exception {
        // Toggle active promo to inactive
        mockMvc.perform(put("/api/v1/promotions/{id}/toggle-status", activeOneTimePromo.getId())
                        .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeOneTimePromo.getId()))
                .andExpect(jsonPath("$.name").value(activeOneTimePromo.getName()));

        // Verify it was changed in the database
        PromoCode updatedPromo = promoCodeRepository.findById(activeOneTimePromo.getId()).orElse(null);
        assertNotNull(updatedPromo);
        assertFalse(updatedPromo.getIsActive());

        // Toggle it back to active
        mockMvc.perform(put("/api/v1/promotions/{id}/toggle-status", activeOneTimePromo.getId())
                        .param("isActive", "true"))
                .andExpect(status().isOk());

        // Verify it was changed back
        updatedPromo = promoCodeRepository.findById(activeOneTimePromo.getId()).orElse(null);
        assertNotNull(updatedPromo);
        assertTrue(updatedPromo.getIsActive());
    }

    @Test
    void toggleNonExistentPromoCodeStatus_fails() throws Exception {
        assertThrows(ServletException.class, () -> {
            mockMvc.perform(put("/api/v1/promotions/{id}/toggle-status", 9999L)
                    .param("isActive", "false"));
        });
    }
}