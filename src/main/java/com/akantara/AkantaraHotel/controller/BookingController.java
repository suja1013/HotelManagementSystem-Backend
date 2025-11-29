package com.akantara.AkantaraHotel.controller;

import com.akantara.AkantaraHotel.dto.Response;
import com.akantara.AkantaraHotel.entity.Booking;
import com.akantara.AkantaraHotel.service.ServiceInterface.BookingServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    // Injects the BookingService class
    @Autowired
    private BookingServiceInterface bookingService;

    // Creates a new booking for a specific room and user and can be accessed by both USER and ADMIN
    @PostMapping("/book-room/{roomId}/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('USER')")
    public ResponseEntity<Response> saveBookings(@PathVariable Long roomId,
                                                 @PathVariable Long userId,
                                                 @RequestBody Booking bookingRequest) {

        Response response = bookingService.saveBooking(roomId, userId, bookingRequest);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Only Admin can retrieve all the bookings
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getAllBookings() {
        Response response = bookingService.getAllBookings();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Retrieves a booking by its confirmation code
    @GetMapping("/get-by-confirmation-code/{confirmationCode}")
    public ResponseEntity<Response> getBookingByConfirmationCode(@PathVariable String confirmationCode) {
        Response response = bookingService.findBookingByConfirmationCode(confirmationCode);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


    // Cancels a booking by booking ID by ADMIN
    @DeleteMapping("/delete/{bookingId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> deleteBooking(@PathVariable Long bookingId) {
        Response response = bookingService.deleteBooking(bookingId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


}
