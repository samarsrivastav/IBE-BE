package backend.service.impl;

import backend.dto.request.CheckoutRequestDTO;
import backend.dto.response.CheckoutResponseDTO;
import backend.entity.Checkout;
import backend.repository.CheckoutRepository;
import backend.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final CheckoutRepository checkoutRepository;

    @Override
    @Transactional
    public CheckoutResponseDTO createCheckout(CheckoutRequestDTO request) {
        log.info("Creating new checkout for room type: {}", request.getRoomTypeId());
        
        Checkout checkout = new Checkout();
        checkout.setRoomTypeId(request.getRoomTypeId());
        checkout.setFirstName(request.getFirstName());
        checkout.setLastName(request.getLastName());
        checkout.setPhoneNumber(request.getPhoneNumber());
        checkout.setEmail(request.getEmail());
        checkout.setMailingAddress1(request.getMailingAddress1());
        checkout.setMailingAddress2(request.getMailingAddress2());
        checkout.setCountry(request.getCountry());
        checkout.setCity(request.getCity());
        checkout.setState(request.getState());
        checkout.setZipCode(request.getZipCode());
        checkout.setCardNumber(request.getCardNumber());
        checkout.setExpiryMonth(request.getExpiryMonth());
        checkout.setExpiryYear(request.getExpiryYear());
        checkout.setCvvCode(request.getCvvCode());

        Checkout savedCheckout = checkoutRepository.save(checkout);
        log.info("Checkout created successfully with ID: {}", savedCheckout.getId());

        return new CheckoutResponseDTO(savedCheckout.getId());
    }

    @Override
    public CheckoutResponseDTO getCheckoutById(Long id) {
        log.info("Fetching checkout with ID: {}", id);
        Checkout checkout = checkoutRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Checkout not found with ID: " + id));
        return new CheckoutResponseDTO(checkout.getId());
    }
} 