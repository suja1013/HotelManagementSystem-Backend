package com.akantara.AkantaraHotel.controller;

import com.akantara.AkantaraHotel.dto.PricingResponseDTO;
import com.akantara.AkantaraHotel.entity.Room;
import com.akantara.AkantaraHotel.exception.CustomException;
import com.akantara.AkantaraHotel.repository.RoomRepository;
import com.akantara.AkantaraHotel.service.implementation.DynamicPricingService;
import com.akantara.AkantaraHotel.service.implementation.DynamicPricingService.PricingBreakdown;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/pricing")
public class PricingController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DynamicPricingService dynamicPricingService;

    /**
     * Preview the dynamic price for a given room and check-in date.
     *
     * @param roomId      ID of the room to price
     * @param checkInDate the intended check-in date (ISO format: YYYY-MM-DD)
     * @return PricingResponseDTO with full factor breakdown and final price
     */
    @GetMapping("/calculate")
    public ResponseEntity<?> calculateDynamicPrice(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate
    ) {
        // Validate inputs
        if (roomId == null || checkInDate == null) {
            return ResponseEntity.badRequest().body("Please provide both roomId and checkInDate");
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body("checkInDate must not be in the past");
        }

        try {
            // Load room to get its base rate (no changes to Room entity or service)
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new CustomException("Room Not Found"));

            BigDecimal baseRate = room.getRoomPrice();

            // Get individual factor breakdown for transparency
            PricingBreakdown breakdown = dynamicPricingService.getPricingBreakdown(checkInDate);

            // Compute the final dynamic price
            BigDecimal dynamicPrice = dynamicPricingService.calculateDynamicPrice(baseRate, checkInDate);

            PricingResponseDTO dto = new PricingResponseDTO(
                    roomId,
                    baseRate,
                    breakdown.demandFactor(),
                    breakdown.timeFactor(),
                    breakdown.weatherFactor(),
                    dynamicPrice
            );

            return ResponseEntity.ok(dto);

        } catch (CustomException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error calculating price: " + e.getMessage());
        }
    }
}
