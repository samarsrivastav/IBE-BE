package backend.controller;

import backend.service.BookedRoomsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/booked-rooms")
public class BookedRoomsController {

    private final BookedRoomsService bookedRoomsService;

    @Autowired
    public BookedRoomsController(BookedRoomsService bookedRoomsService) {
        this.bookedRoomsService = bookedRoomsService;
    }

    @GetMapping("/check-in/{propertyId}")
    public List<Long> getCheckInRoomsForToday(@PathVariable int propertyId) {
        return bookedRoomsService.getCheckInRoomsForToday(propertyId);
    }

    @GetMapping("/check-out/{propertyId}")
    public List<Long> getCheckOutRoomsForToday(@PathVariable int propertyId) {
        return bookedRoomsService.getCheckOutRoomsForToday(propertyId);
    }

    @GetMapping("/current/{propertyId}")
    public List<Long> getCurrentlyBookedRooms(@PathVariable int propertyId) {
        return bookedRoomsService.getCurrentlyBookedRooms(propertyId);
    }
} 