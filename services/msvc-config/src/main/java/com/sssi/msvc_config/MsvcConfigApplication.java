package com.sssi.msvc_config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

// It allows the application to serve configuration properties to other microservices in a centralized manner.
@EnableConfigServer
@SpringBootApplication
public class MsvcConfigApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsvcConfigApplication.class, args);
	}

}
