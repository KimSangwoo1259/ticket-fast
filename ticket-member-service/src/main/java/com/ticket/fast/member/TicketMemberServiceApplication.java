package com.ticket.fast.member;

import com.ticket.fast.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "com.ticket.fast")
public class TicketMemberServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketMemberServiceApplication.class, args);
	}

}
