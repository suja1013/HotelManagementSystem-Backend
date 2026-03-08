package com.akantara.AkantaraHotel.service.implementation;

import com.akantara.AkantaraHotel.dto.BookingDTO;
import com.akantara.AkantaraHotel.dto.Response;
import com.akantara.AkantaraHotel.entity.Booking;
import com.akantara.AkantaraHotel.entity.Room;
import com.akantara.AkantaraHotel.entity.User;
import com.akantara.AkantaraHotel.exception.CustomException;
import com.akantara.AkantaraHotel.repository.BookingRepository;
import com.akantara.AkantaraHotel.repository.RoomRepository;
import com.akantara.AkantaraHotel.repository.UserRepository;
import com.akantara.AkantaraHotel.service.ServiceInterface.BookingServiceInterface;
import com.akantara.AkantaraHotel.service.ServiceInterface.RoomServiceInterface;
import com.akantara.AkantaraHotel.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BookingService implements BookingServiceInterface {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DynamicPricingService dynamicPricingService;


    // Saves a new booking for a given room and user
    @Override
    public Response saveBooking(Long roomId, Long userId, Booking bookingRequest) {

        Response response = new Response();

        try {
            // Validation: check-out date cannot be before check-in date
            if (bookingRequest.getCheckOutDate().isBefore(bookingRequest.getCheckInDate())) {
                throw new IllegalArgumentException("check-out date cannot be before check-in date");
            }

            // Fetch room and user from database (throws exception if not found)
            Room room = roomRepository.findById(roomId).orElseThrow(() -> new CustomException("Room Not Found"));
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException("User Not Found"));

            List<Booking> existingBookings = room.getBookings();

            // Check if room is available for given date range
            if (!roomIsAvailable(bookingRequest, existingBookings)) {
                throw new CustomException("Room not available for selected date range");
            }

            bookingRequest.setRoom(room);
            bookingRequest.setUser(user);
            BigDecimal dynamicPrice = dynamicPricingService.calculateDynamicPrice(
                    room.getRoomPrice(), bookingRequest.getCheckInDate());
            long numberOfNights = bookingRequest.getCheckInDate()
                    .until(bookingRequest.getCheckOutDate(), java.time.temporal.ChronoUnit.DAYS);
            BigDecimal totalPrice = dynamicPrice.multiply(BigDecimal.valueOf(numberOfNights));
            bookingRequest.setTotalPrice(totalPrice);

            String bookingConfirmationCode = Utils.generateRandomConfirmationCode( );
            bookingRequest.setBookingConfirmationCode(bookingConfirmationCode);

            bookingRepository.save(bookingRequest);
            response.setStatusCode(200);
            response.setMessage("successful");
            response.setBookingConfirmationCode(bookingConfirmationCode);

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());

        }
        return response;
    }

    // Find booking by booking confirmation code
    @Override
    public Response findBookingByConfirmationCode(String confirmationCode) {

        Response response = new Response();

        try {
            Booking booking = bookingRepository.findByBookingConfirmationCode(confirmationCode).orElseThrow(() -> new CustomException("Booking Not Found"));
            BookingDTO bookingDTO = Utils.mapBookingEntityToBookingDTOPlusBookedRooms(booking, true);
            response.setStatusCode(200);
            response.setMessage("successful");
            response.setBooking(bookingDTO);

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());

        }
        return response;
    }

    // Fetch all bookings from the database
    @Override
    public Response getAllBookings() {

        Response response = new Response();

        try {
            List<Booking> bookingList = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
            List<BookingDTO> bookingDTOList = Utils.mapBookingListEntityToBookingListDTO(bookingList);
            response.setStatusCode(200);
            response.setMessage("successful");
            response.setBookings(bookingDTOList);

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());

        }
        return response;
    }

    // Deletes an existing booking by ID
    @Override
    public Response deleteBooking(Long bookingId) {

        Response response = new Response();

        try {
            bookingRepository.findById(bookingId).orElseThrow(() -> new CustomException("Booking Does Not Exist"));
            bookingRepository.deleteById(bookingId);
            response.setStatusCode(200);
            response.setMessage("successful");

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());

        }
        return response;
    }


    //Checks if the room is available for the given check-in and check-out dates, returns true or false
    private boolean roomIsAvailable(Booking bookingRequest, List<Booking> existingBookings) {

        return existingBookings.stream()
                .noneMatch(existingBooking ->
                        bookingRequest.getCheckInDate().equals(existingBooking.getCheckInDate())
                                || bookingRequest.getCheckOutDate().isBefore(existingBooking.getCheckOutDate())
                                || (bookingRequest.getCheckInDate().isAfter(existingBooking.getCheckInDate())
                                && bookingRequest.getCheckInDate().isBefore(existingBooking.getCheckOutDate()))
                                || (bookingRequest.getCheckInDate().isBefore(existingBooking.getCheckInDate())

                                && bookingRequest.getCheckOutDate().equals(existingBooking.getCheckOutDate()))
                                || (bookingRequest.getCheckInDate().isBefore(existingBooking.getCheckInDate())

                                && bookingRequest.getCheckOutDate().isAfter(existingBooking.getCheckOutDate()))

                                || (bookingRequest.getCheckInDate().equals(existingBooking.getCheckOutDate())
                                && bookingRequest.getCheckOutDate().equals(existingBooking.getCheckInDate()))

                                || (bookingRequest.getCheckInDate().equals(existingBooking.getCheckOutDate())
                                && bookingRequest.getCheckOutDate().equals(bookingRequest.getCheckInDate()))
                );
    }
}
