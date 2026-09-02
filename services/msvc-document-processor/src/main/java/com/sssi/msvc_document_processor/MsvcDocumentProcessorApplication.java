package com.sssi.msvc_document_processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsvcDocumentProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsvcDocumentProcessorApplication.class, args);
	}

}
