package com.sssi.msvc_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// That service is going to be a client of Eureka,
// so we need to add the annotation @EnableDiscoveryClient
@EnableDiscoveryClient
@SpringBootApplication
public class MsvcAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsvcAuthApplication.class, args);
	}

}
