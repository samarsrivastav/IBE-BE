package IBE_Backend.service;

import IBE_Backend.dto.PropertyDTO;
import IBE_Backend.entity.Property;
import IBE_Backend.exception.ResourceNotFoundException;
import IBE_Backend.repository.PropertyRepository;
import IBE_Backend.service.impl.PropertyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PropertyServiceImplTest {

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private Property property1;
    private Property property2;
    private PropertyDTO propertyDTO1;

    @BeforeEach
    void setUp() {
        // Setup test data
        property1 = new Property("Test Property 1", "123 Test Street");
        property1.setId(1L);

        property2 = new Property("Test Property 2", "456 Test Avenue");
        property2.setId(2L);

        propertyDTO1 = new PropertyDTO(1L, "Test Property 1", "123 Test Street");
    }

    @Test
    void testGetAllProperties() {
        // Given
        when(propertyRepository.findAll()).thenReturn(Arrays.asList(property1, property2));

        // When
        List<PropertyDTO> result = propertyService.getAllProperties();

        // Then
        assertEquals(2, result.size());
        assertEquals("Test Property 1", result.get(0).getName());
        assertEquals("Test Property 2", result.get(1).getName());
        verify(propertyRepository, times(1)).findAll();
    }

    @Test
    void testGetPropertyById() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property1));

        // When
        PropertyDTO result = propertyService.getPropertyById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Property 1", result.getName());
        assertEquals("123 Test Street", result.getAddress());
        verify(propertyRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPropertyById_NotFound() {
        // Given
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            propertyService.getPropertyById(99L);
        });
        verify(propertyRepository, times(1)).findById(99L);
    }

    @Test
    void testCreateProperty() {
        // Given
        when(propertyRepository.save(any(Property.class))).thenReturn(property1);

        // When
        PropertyDTO result = propertyService.createProperty(propertyDTO1);

        // Then
        assertNotNull(result);
        assertEquals("Test Property 1", result.getName());
        verify(propertyRepository, times(1)).save(any(Property.class));
    }

    @Test
    void testUpdateProperty() {
        // Given
        PropertyDTO updateDTO = new PropertyDTO(1L, "Updated Property", "Updated Address");
        Property updatedProperty = new Property("Updated Property", "Updated Address");
        updatedProperty.setId(1L);

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property1));
        when(propertyRepository.save(any(Property.class))).thenReturn(updatedProperty);

        // When
        PropertyDTO result = propertyService.updateProperty(1L, updateDTO);

        // Then
        assertNotNull(result);
        assertEquals("Updated Property", result.getName());
        assertEquals("Updated Address", result.getAddress());
        verify(propertyRepository, times(1)).findById(1L);
        verify(propertyRepository, times(1)).save(any(Property.class));
    }

    @Test
    void testDeleteProperty() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property1));
        doNothing().when(propertyRepository).delete(any(Property.class));

        // When
        propertyService.deleteProperty(1L);

        // Then
        verify(propertyRepository, times(1)).findById(1L);
        verify(propertyRepository, times(1)).delete(any(Property.class));
    }
}