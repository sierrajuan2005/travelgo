package com.travelgo.travelgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.travelgo")
@EntityScan(basePackages = "com.travelgo.model")
@EnableJpaRepositories(basePackages = "com.travelgo.repository")
public class TravelgoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelgoApplication.class, args);
	}
}