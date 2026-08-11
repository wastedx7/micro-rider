package com.ridehailing.ws.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DriverWebsocketServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverWebsocketServerApplication.class, args);
    }
}