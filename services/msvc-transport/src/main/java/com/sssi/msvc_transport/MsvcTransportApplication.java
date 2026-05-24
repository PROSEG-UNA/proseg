package com.sssi.msvc_transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class MsvcTransportApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcTransportApplication.class, args);
    }
}