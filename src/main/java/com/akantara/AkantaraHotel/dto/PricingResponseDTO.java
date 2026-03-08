package com.akantara.AkantaraHotel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PricingResponseDTO {

    private Long roomId;
    private BigDecimal baseRate;
    private double demandFactor;
    private double timeFactor;
    private double weatherFactor;
    private BigDecimal dynamicPrice;

    public PricingResponseDTO() {}

    public PricingResponseDTO(Long roomId, BigDecimal baseRate,
                              double demandFactor, double timeFactor,
                              double weatherFactor, BigDecimal dynamicPrice) {
        this.roomId        = roomId;
        this.baseRate      = baseRate;
        this.demandFactor  = demandFactor;
        this.timeFactor    = timeFactor;
        this.weatherFactor = weatherFactor;
        this.dynamicPrice  = dynamicPrice;
    }


    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }

    public double getDemandFactor() { return demandFactor; }
    public void setDemandFactor(double demandFactor) { this.demandFactor = demandFactor; }

    public double getTimeFactor() { return timeFactor; }
    public void setTimeFactor(double timeFactor) { this.timeFactor = timeFactor; }

    public double getWeatherFactor() { return weatherFactor; }
    public void setWeatherFactor(double weatherFactor) { this.weatherFactor = weatherFactor; }

    public BigDecimal getDynamicPrice() { return dynamicPrice; }
    public void setDynamicPrice(BigDecimal dynamicPrice) { this.dynamicPrice = dynamicPrice; }

    @Override
    public String toString() {
        return "PricingResponseDTO{" +
                "roomId=" + roomId +
                ", baseRate=" + baseRate +
                ", demandFactor=" + demandFactor +
                ", timeFactor=" + timeFactor +
                ", weatherFactor=" + weatherFactor +
                ", dynamicPrice=" + dynamicPrice +
                '}';
    }
}
