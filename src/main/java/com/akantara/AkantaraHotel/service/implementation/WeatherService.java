package com.akantara.AkantaraHotel.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    // Injected from application.properties: weather.api.key
    @Value("${weather.api.key:}")
    private String apiKey;

    // Injected from application.properties: weather.city (default: Jakarta)
    @Value("${weather.city:Jakarta}")
    private String city;

    private static final String WEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather?q={city}&appid={apiKey}&units=metric";

    private final RestTemplate restTemplate = new RestTemplate();

    public double getWeatherFactor() {
        // Skip the API call if no key has been configured
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("WeatherService: No API key configured (weather.api.key). Using neutral factor 1.0");
            return 1.0;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                    WEATHER_URL, Map.class, city, apiKey);

            if (response == null) {
                log.warn("WeatherService: Empty response from OpenWeatherMap. Using neutral factor 1.0");
                return 1.0;
            }

            // Extract the main weather condition string from the response payload
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> weatherList =
                    (java.util.List<Map<String, Object>>) response.get("weather");

            if (weatherList == null || weatherList.isEmpty()) {
                return 1.0;
            }

            String mainCondition = (String) weatherList.get(0).get("main");
            double factor = mapConditionToFactor(mainCondition);

            log.info("WeatherService: city={}, condition={}, WeatherFactor={}", city, mainCondition, factor);
            return factor;

        } catch (Exception e) {
            log.error("WeatherService: Failed to fetch weather data. Using neutral factor 1.0. Reason: {}", e.getMessage());
            return 1.0;
        }
    }

    private double mapConditionToFactor(String condition) {
        if (condition == null) return 1.0;
        return switch (condition) {
            case "Clear"        -> 1.15;   // Sunny — travellers more likely to book; increase price
            case "Clouds"       -> 1.05;   // Overcast — mild increase
            case "Drizzle",
                 "Rain"         -> 0.90;   // Rainy — lower desirability; slight discount
            case "Thunderstorm" -> 0.85;   // Storm — significant demand drop
            case "Snow",
                 "Extreme"      -> 0.80;   // Severe weather — biggest discount
            default             -> 1.00;   // Mist, Haze, Fog, etc. — neutral
        };
    }
}
