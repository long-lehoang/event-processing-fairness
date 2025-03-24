package com.event.processing.notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Event Processing Notifier service.
 * This Spring Boot application handles event processing and notification
 * delivery.
 * 
 * The application is structured with the following main components:
 * - Consumer: Handles incoming event messages
 * - Producer: Manages outgoing notifications
 * - Service: Contains business logic for event processing
 * - Domain: Defines core business entities and models
 * - Config: Contains application configuration
 * - Monitoring: Handles application metrics and monitoring
 * - Util: Contains utility classes and helper functions
 *
 * @author LongLe
 * @version 1.0
 */
@SpringBootApplication
public class NotiferApplication {

  /**
   * Main entry point for the application.
   * Initializes and starts the Spring Boot application context.
   *
   * @param args Command line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(NotiferApplication.class, args);
  }

}
