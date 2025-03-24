package backend.service;

import backend.dto.PropertyDTO;
import backend.entity.Property;
import backend.exception.ResourceNotFoundException;
import backend.repository.PropertyRepository;

import backend.service.impl.PropertyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropertyServiceImplTest {

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private Property sampleProperty;
    private PropertyDTO samplePropertyDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleProperty = new Property();
        sampleProperty.setId(1L);
        sampleProperty.setName("Sample Property");
        sampleProperty.setAddress("123 Main Street");

        samplePropertyDTO = new PropertyDTO(1L, "Sample Property", "123 Main Street");
    }

    @Test
    void getAllProperties_ShouldReturnListOfProperties() {
        // Given
        when(propertyRepository.findAll()).thenReturn(List.of(sampleProperty));

        // When
        List<PropertyDTO> result = propertyService.getAllProperties();

        // Then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(samplePropertyDTO.getId(), result.get(0).getId());
        verify(propertyRepository, times(1)).findAll();
    }

    @Test
    void getPropertyById_ShouldReturnProperty_WhenFound() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(sampleProperty));

        // When
        PropertyDTO result = propertyService.getPropertyById(1L);

        // Then
        assertNotNull(result);
        assertEquals(samplePropertyDTO.getId(), result.getId());
        verify(propertyRepository, times(1)).findById(1L);
    }

    @Test
    void getPropertyById_ShouldThrowException_WhenNotFound() {
        // Given
        when(propertyRepository.findById(2L)).thenReturn(Optional.empty());

        // Then
        assertThrows(ResourceNotFoundException.class, () -> propertyService.getPropertyById(2L));
        verify(propertyRepository, times(1)).findById(2L);
    }

    @Test
    void createProperty_ShouldSaveAndReturnProperty() {
        // Given
        when(propertyRepository.save(any(Property.class))).thenReturn(sampleProperty);

        // When
        PropertyDTO result = propertyService.createProperty(samplePropertyDTO);

        // Then
        assertNotNull(result);
        assertEquals(samplePropertyDTO.getId(), result.getId());
        verify(propertyRepository, times(1)).save(any(Property.class));
    }

    @Test
    void updateProperty_ShouldUpdateAndReturnProperty_WhenFound() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(sampleProperty));
        when(propertyRepository.save(any(Property.class))).thenReturn(sampleProperty);

        PropertyDTO updatedDTO = new PropertyDTO(1L, "Updated Name", "Updated Address");

        // When
        PropertyDTO result = propertyService.updateProperty(1L, updatedDTO);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Address", result.getAddress());
        verify(propertyRepository, times(1)).findById(1L);
        verify(propertyRepository, times(1)).save(any(Property.class));
    }

    @Test
    void updateProperty_ShouldThrowException_WhenNotFound() {
        // Given
        when(propertyRepository.findById(2L)).thenReturn(Optional.empty());

        PropertyDTO updatedDTO = new PropertyDTO(2L, "Updated Name", "Updated Address");

        // Then
        assertThrows(ResourceNotFoundException.class, () -> propertyService.updateProperty(2L, updatedDTO));
        verify(propertyRepository, times(1)).findById(2L);
        verify(propertyRepository, never()).save(any(Property.class));
    }

    @Test
    void deleteProperty_ShouldDeleteProperty_WhenFound() {
        // Given
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(sampleProperty));
        doNothing().when(propertyRepository).delete(sampleProperty);

        // When
        propertyService.deleteProperty(1L);

        // Then
        verify(propertyRepository, times(1)).findById(1L);
        verify(propertyRepository, times(1)).delete(sampleProperty);
    }

    @Test
    void deleteProperty_ShouldThrowException_WhenNotFound() {
        // Given
        when(propertyRepository.findById(2L)).thenReturn(Optional.empty());

        // Then
        assertThrows(ResourceNotFoundException.class, () -> propertyService.deleteProperty(2L));
        verify(propertyRepository, times(1)).findById(2L);
        verify(propertyRepository, never()).delete(any(Property.class));
    }
}
