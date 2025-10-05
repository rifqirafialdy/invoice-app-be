package com.invoiceapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvoiceManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoiceManagementApplication.class, args);
	}

}
