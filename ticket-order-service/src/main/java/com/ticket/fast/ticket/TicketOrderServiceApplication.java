package com.ticket.fast.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ticket.fast")
public class TicketOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketOrderServiceApplication.class, args);
    }

}
