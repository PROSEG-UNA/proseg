package com.sssi.msvc_maintenance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class MsvcMaintenanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcMaintenanceApplication.class, args);
    }

}