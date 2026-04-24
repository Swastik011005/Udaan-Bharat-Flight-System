package com.udaanbharat.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point.
 * @EnableScheduling — auto-complete departed bookings every 10 min
 * @EnableAsync      — email sending runs in a background thread pool
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AirlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirlineApplication.class, args);
    }
}