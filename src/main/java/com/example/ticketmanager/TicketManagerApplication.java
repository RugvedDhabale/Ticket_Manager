package com.example.ticketmanager;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.ticketmanager.entity.User;
import com.example.ticketmanager.repository.UserRepository;

@SpringBootApplication
public class TicketManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketManagerApplication.class, args);
	}

	@Bean
	CommandLineRunner initUsers(UserRepository repo, PasswordEncoder encoder) {
		return args -> {
			repo.deleteAll();

			User admin = new User();
			admin.setUsername("admin");
			admin.setPassword(encoder.encode("admin123"));
			admin.setRole("ADMIN");
			repo.save(admin);

			System.out.println("ADMIN USER CREATED");
		};
	}
}
