package com.event.processing.dlq_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DlqServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DlqServiceApplication.class, args);
  }

}
