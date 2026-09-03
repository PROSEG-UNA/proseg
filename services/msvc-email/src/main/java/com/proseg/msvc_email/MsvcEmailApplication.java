package com.proseg.msvc_email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsvcEmailApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsvcEmailApplication.class, args);
	}

}
