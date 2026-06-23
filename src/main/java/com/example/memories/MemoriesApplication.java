package com.example.memories;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MemoriesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemoriesApplication.class, args);
	}

}
