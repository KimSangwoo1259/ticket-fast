package com.ticket.fast.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ticket.fast")
public class TicketCommonApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketCommonApplication.class, args);
    }

}
