package com.akantara.AkantaraHotel.service.implementation;

import com.akantara.AkantaraHotel.dto.Response;
import com.akantara.AkantaraHotel.entity.Booking;
import com.akantara.AkantaraHotel.exception.CustomException;
import com.akantara.AkantaraHotel.repository.BookingRepository;
import com.akantara.AkantaraHotel.repository.RoomRepository;
import com.akantara.AkantaraHotel.service.ServiceInterface.CancellationServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


@Service
public class CancellationService implements CancellationServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(CancellationService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;


     //Cancels a booking and computes the refund amount.
     //The booking record is deleted after computing the refund.
    public Response cancelBooking(Long bookingId) {
        Response response = new Response();

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new CustomException("Booking Not Found"));

            LocalDate checkInDate = booking.getCheckInDate();
            long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);

            // Cannot cancel a booking that has already started or passed
            if (daysUntilCheckIn < 0) {
                throw new CustomException("Cannot cancel a booking that has already started or passed.");
            }

            BigDecimal totalPrice = booking.getTotalPrice();
            if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) == 0) {
                // If no price was recorded, cancel without refund
                bookingRepository.deleteById(bookingId);
                response.setStatusCode(200);
                response.setMessage("Booking cancelled. No refund applicable (no price recorded).");
                response.setCancellationRefund(BigDecimal.ZERO);
                return response;
            }

            // Step 1: base refund rate from days until check-in
            double baseRefundRate = calculateBaseRefundRate(daysUntilCheckIn);

            // Step 2: demand adjustment for this room
            double demandAdjustment = calculateDemandAdjustment(booking.getRoom().getId());

            // Step 3: final refund rate clamped between 0 and 1
            double finalRefundRate = Math.max(0.0, Math.min(1.0, baseRefundRate + demandAdjustment));

            // Step 4: compute refund amount
            BigDecimal refundAmount = totalPrice
                    .multiply(BigDecimal.valueOf(finalRefundRate))
                    .setScale(2, RoundingMode.HALF_UP);

            log.info("CancellationService: bookingId={}, daysUntilCheckIn={}, baseRefundRate={}, " +
                            "demandAdjustment={}, finalRefundRate={}, refundAmount={}",
                    bookingId, daysUntilCheckIn, baseRefundRate, demandAdjustment, finalRefundRate, refundAmount);

            // Delete the booking
            bookingRepository.deleteById(bookingId);

            // Build human-readable message
            String message = buildCancellationMessage(daysUntilCheckIn, baseRefundRate, demandAdjustment, finalRefundRate, refundAmount);

            response.setStatusCode(200);
            response.setMessage(message);
            response.setCancellationRefund(refundAmount);

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error processing cancellation: " + e.getMessage());
        }

        return response;
    }
     // Preview: returns the refund amount without actually cancelling.
     //Used by GET /bookings/cancel-preview/{bookingId}

    public Response previewCancellation(Long bookingId) {
        Response response = new Response();

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new CustomException("Booking Not Found"));

            LocalDate checkInDate = booking.getCheckInDate();
            long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);

            if (daysUntilCheckIn < 0) {
                response.setStatusCode(400);
                response.setMessage("This booking has already started or passed. Cancellation not allowed.");
                response.setCancellationRefund(BigDecimal.ZERO);
                return response;
            }

            BigDecimal totalPrice = booking.getTotalPrice();
            if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) == 0) {
                response.setStatusCode(200);
                response.setMessage("No refund applicable (no price recorded).");
                response.setCancellationRefund(BigDecimal.ZERO);
                return response;
            }

            double baseRefundRate   = calculateBaseRefundRate(daysUntilCheckIn);
            double demandAdjustment = calculateDemandAdjustment(booking.getRoom().getId());
            double finalRefundRate  = Math.max(0.0, Math.min(1.0, baseRefundRate + demandAdjustment));

            BigDecimal refundAmount = totalPrice
                    .multiply(BigDecimal.valueOf(finalRefundRate))
                    .setScale(2, RoundingMode.HALF_UP);

            String message = buildCancellationMessage(daysUntilCheckIn, baseRefundRate, demandAdjustment, finalRefundRate, refundAmount);

            response.setStatusCode(200);
            response.setMessage(message);
            response.setCancellationRefund(refundAmount);

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error previewing cancellation: " + e.getMessage());
        }

        return response;
    }

    // Private helpers

    private double calculateBaseRefundRate(long daysUntilCheckIn) {
        if (daysUntilCheckIn > 30) return 1.00;    // 100% full refund
        if (daysUntilCheckIn >= 15) return 0.95;   // 95% refund
        if (daysUntilCheckIn >= 7)  return 0.85;   // 85% refund
        if (daysUntilCheckIn >= 3)  return 0.75;   // 75% refund
        return 0.50;                                // 50% refund (last-minute)
    }

    private double calculateDemandAdjustment(Long roomId) {
        long totalRooms = roomRepository.count();
        if (totalRooms == 0) return 0.0;

        long bookedRooms = Math.min(bookingRepository.count(), totalRooms);
        double availabilityPercent = ((double)(totalRooms - bookedRooms) / totalRooms) * 100.0;

        if (availabilityPercent < 20) return -0.05;  // Very high demand → −5% nudge
        if (availabilityPercent < 50) return -0.03;  // High demand      → −3% nudge
        if (availabilityPercent < 80) return  0.00;  // Normal demand    → no change
        return +0.05;                                 // Low demand       → +5% bonus
    }

     //Builds a friendly human-readable cancellation message that explains
     //BOTH the time-based refund rate AND the demand adjustment applied.

    private String buildCancellationMessage(long daysUntilCheckIn, double baseRefundRate,
                                            double demandAdjustment, double finalRefundRate,
                                            BigDecimal refundAmount) {
        // Part 1: time context
        String timeContext;
        if (daysUntilCheckIn > 30) {
            timeContext = "You are cancelling well in advance (" + daysUntilCheckIn + " days before check-in), "
                    + "which qualifies for a " + Math.round(baseRefundRate * 100) + "% base refund.";
        } else if (daysUntilCheckIn >= 15) {
            timeContext = "You are cancelling " + daysUntilCheckIn + " days before check-in, "
                    + "which qualifies for a " + Math.round(baseRefundRate * 100) + "% base refund.";
        } else if (daysUntilCheckIn >= 7) {
            timeContext = "You are cancelling " + daysUntilCheckIn + " days before check-in — "
                    + "within the 7–14 day window, which qualifies for a " + Math.round(baseRefundRate * 100) + "% base refund.";
        } else if (daysUntilCheckIn >= 3) {
            timeContext = "Late cancellation — only " + daysUntilCheckIn + " days before check-in. "
                    + "Base refund is " + Math.round(baseRefundRate * 100) + "%.";
        } else {
            timeContext = "Last-minute cancellation (" + daysUntilCheckIn + " day(s) before check-in). "
                    + "A " + Math.round(baseRefundRate * 100) + "% base refund applies.";
        }

        // Part 2: demand context
        String demandContext;
        if (demandAdjustment <= -0.05) {
            demandContext = "The room is in very high demand right now, "
                    + "so a small 5% demand penalty has been applied.";
        } else if (demandAdjustment <= -0.03) {
            demandContext = "The room is in high demand at the moment, "
                    + "so a small 3% demand penalty has been applied.";
        } else if (demandAdjustment >= 0.05) {
            demandContext = "Room demand is currently low, making it easy to rebook, "
                    + "so a 5% bonus has been added to your refund.";
        } else {
            demandContext = "Room demand is normal — no demand-based adjustment applied.";
        }

        // Part 3: final summary
        int finalPercent = (int) Math.round(finalRefundRate * 100);
        String summary = "Your final refund is " + finalPercent + "% → $" + refundAmount + ".";

        return timeContext + " " + demandContext + " " + summary;
    }
}
