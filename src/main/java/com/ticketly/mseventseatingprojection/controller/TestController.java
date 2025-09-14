package com.ticketly.mseventseatingprojection.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    /**
     * Simple test endpoint to verify the service is running.
     *
     * @return "Hello, World!" string.
     */
    @GetMapping
    public String test() {
        return "Hello, World!";
    }
}
