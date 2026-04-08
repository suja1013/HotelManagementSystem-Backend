package com.akantara.AkantaraHotel.controller;

import com.akantara.AkantaraHotel.dto.Response;
import com.akantara.AkantaraHotel.entity.Booking;
import com.akantara.AkantaraHotel.service.ServiceInterface.BookingServiceInterface;
import com.akantara.AkantaraHotel.service.ServiceInterface.CancellationServiceInterface;
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

    @Autowired
    private CancellationServiceInterface cancellationService;

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

    // Preview refund amount before confirming cancellation (USER or ADMIN)
    @GetMapping("/cancel-preview/{bookingId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('USER')")
    public ResponseEntity<Response> previewCancellation(@PathVariable Long bookingId) {
        Response response = cancellationService.previewCancellation(bookingId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Confirm cancellation — computes refund, deletes booking (USER or ADMIN)
    @DeleteMapping("/cancel/{bookingId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('USER')")
    public ResponseEntity<Response> cancelBooking(@PathVariable Long bookingId) {
        Response response = cancellationService.cancelBooking(bookingId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
