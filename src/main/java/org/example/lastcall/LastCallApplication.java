package org.example.lastcall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LastCallApplication {
	public static void main(String[] args) {
		SpringApplication.run(LastCallApplication.class, args);
	}
}
