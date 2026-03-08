package com.akantara.AkantaraHotel.service.implementation;

import com.akantara.AkantaraHotel.repository.BookingRepository;
import com.akantara.AkantaraHotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DynamicPricingService {

    private static final Logger log = LoggerFactory.getLogger(DynamicPricingService.class);

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WeatherService weatherService;


    public BigDecimal calculateDynamicPrice(BigDecimal baseRate, LocalDate checkInDate) {

        double demandFactor  = calculateDemandFactor();
        double timeFactor    = calculateTimeFactor(checkInDate);
        double weatherFactor = weatherService.getWeatherFactor();

        double multiplier = demandFactor * timeFactor * weatherFactor;

        BigDecimal dynamicPrice = baseRate
                .multiply(BigDecimal.valueOf(multiplier))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("DynamicPricing: baseRate={}, demandFactor={}, timeFactor={}, weatherFactor={}, finalPrice={}",
                baseRate, demandFactor, timeFactor, weatherFactor, dynamicPrice);

        return dynamicPrice;
    }

    /**
     * Returns an informational breakdown of the pricing factors for a given request.
     * Used by the /pricing/calculate endpoint to give transparency to callers.
     */
    public PricingBreakdown getPricingBreakdown(LocalDate checkInDate) {
        double demandFactor  = calculateDemandFactor();
        double timeFactor    = calculateTimeFactor(checkInDate);
        double weatherFactor = weatherService.getWeatherFactor();
        return new PricingBreakdown(demandFactor, timeFactor, weatherFactor);
    }

    // ─── Factor calculation helpers ────────────────────────────────────────────

    /**
     * DemandFactor: based on the percentage of rooms that are currently unbooked.
     * Uses the existing RoomRepository to count total rooms and BookingRepository
     * for currently active bookings — no original logic is changed.
     */
    private double calculateDemandFactor() {
        long totalRooms = roomRepository.count();
        if (totalRooms == 0) return 1.0;

        // Rooms that have at least one booking are considered "occupied"
        long bookedRooms = bookingRepository.count();

        // Clamp booked rooms so it never exceeds total
        bookedRooms = Math.min(bookedRooms, totalRooms);

        double availabilityPercent = ((double)(totalRooms - bookedRooms) / totalRooms) * 100.0;

        if (availabilityPercent < 20) {
            return 1.30;   // Very high demand
        } else if (availabilityPercent < 50) {
            return 1.15;   // High demand
        } else if (availabilityPercent < 80) {
            return 1.00;   // Normal demand
        } else {
            return 0.90;   // Low demand — offer a discount
        }
    }

    /**
     * TimeFactor: based on days between today and the requested check-in date.
     */
    private double calculateTimeFactor(LocalDate checkInDate) {
        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);

        if (daysUntilCheckIn <= 1) {
            return 1.20;   // Last-minute booking
        } else if (daysUntilCheckIn <= 7) {
            return 1.10;   // Near-term booking
        } else if (daysUntilCheckIn <= 30) {
            return 1.00;   // Standard window
        } else {
            return 0.90;   // Advance booking — early-bird discount
        }
    }

    // ─── Nested result record ──────────────────────────────────────────────────

    /**
     * Immutable data carrier for the pricing breakdown response.
     */
    public record PricingBreakdown(double demandFactor, double timeFactor, double weatherFactor) {

        /** Convenience method: returns the combined multiplier. */
        public double combinedMultiplier() {
            return demandFactor * timeFactor * weatherFactor;
        }
    }
}
