package com.sssi.msvc_email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// That service is going to be a client of Eureka,
// so we need to add the annotation @EnableDiscoveryClient
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
public class MsvcEmailApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsvcEmailApplication.class, args);
	}

}
