package com.likelion.realtalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RealtalkApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealtalkApplication.class, args);
	}

}
