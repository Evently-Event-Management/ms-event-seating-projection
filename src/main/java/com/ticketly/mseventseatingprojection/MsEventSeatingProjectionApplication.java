package com.ticketly.mseventseatingprojection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsEventSeatingProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsEventSeatingProjectionApplication.class, args);
    }

}
