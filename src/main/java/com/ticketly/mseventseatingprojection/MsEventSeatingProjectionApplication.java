package com.ticketly.mseventseatingprojection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MsEventSeatingProjectionApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MsEventSeatingProjectionApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
