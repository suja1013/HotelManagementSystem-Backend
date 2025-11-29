package com.akantara.AkantaraHotel.controller;

import com.akantara.AkantaraHotel.dto.Response;
import com.akantara.AkantaraHotel.service.ServiceInterface.RoomServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    // Injects the RoomService class
    @Autowired
    private RoomServiceInterface roomService;

    // Admin adds a new room and validates input fields before adding the room in the database
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> addNewRoom(

            @RequestParam(value = "roomType") String roomType,
            @RequestParam(value = "roomPrice") BigDecimal roomPrice,
            @RequestParam(value = "roomDescription") String roomDescription
    )
    {
        if (roomType == null || roomDescription == null || roomPrice == null ) {
            Response response = new Response();
            response.setStatusCode(400);
            response.setMessage("Please provide values for all the fields");
            return ResponseEntity.status(response.getStatusCode()).body(response);
        }

        Response response = roomService.addNewRoom(roomType, roomPrice, roomDescription);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Retrieves all rooms
    @GetMapping("/all")
    public ResponseEntity<Response> getAllRooms() {
        Response response = roomService.getAllRooms();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Retrieves all the room types
    @GetMapping("/types")
    public List<String> getRoomTypes() {
        return roomService.getAllRoomTypes();
    }

    // Retrieves room details by room ID
    @GetMapping("/room-by-id/{roomId}")
    public ResponseEntity<Response> getRoomById(@PathVariable Long roomId) {
        Response response = roomService.getRoomById(roomId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Retrieves available rooms filtered by check-in date, check-out date, and room type and validates input
    @GetMapping("/available-rooms-by-date-and-type")
    public ResponseEntity<Response> getAvailableRoomsByDateAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam String roomType
    ) {
        if (checkInDate == null || roomType == null || roomType.isBlank() || checkOutDate == null) {
            Response response = new Response();
            response.setStatusCode(400);
            response.setMessage("Please provide values for all the fields");
            return ResponseEntity.status(response.getStatusCode()).body(response);
        }
        Response response = roomService.getAvailableRoomsByDataAndType(checkInDate, checkOutDate, roomType);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Only Admin can update the room details by room ID
    @PutMapping("/update/{roomId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> updateRoom(@PathVariable Long roomId,
                                               @RequestParam(value = "roomType", required = false) String roomType,
                                               @RequestParam(value = "roomPrice", required = false) BigDecimal roomPrice,
                                               @RequestParam(value = "roomDescription", required = false) String roomDescription

    ) {
        Response response = roomService.updateRoom(roomId, roomDescription, roomType, roomPrice);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Only Admin can delete the room using room ID
    @DeleteMapping("/delete/{roomId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> deleteRoom(@PathVariable Long roomId) {
        Response response = roomService.deleteRoom(roomId);
        return ResponseEntity.status(response.getStatusCode()).body(response);

    }

}
