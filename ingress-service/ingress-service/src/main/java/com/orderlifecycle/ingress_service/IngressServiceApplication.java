package com.orderlifecycle.ingress_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.orderlifecycle.ingress_service.security.JwtProvider;

@SpringBootApplication
public class IngressServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IngressServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner generateTestToken(JwtProvider jwtProvider) {
		return args -> {
			System.out.println("\n==================================================");
			System.out.println("TOKEN DE PRUEBA PARA POSTMAN:");
			System.out.println("Bearer " + jwtProvider.generateToken("test-user-123"));
			System.out.println("==================================================\n");
		};
	}

}
