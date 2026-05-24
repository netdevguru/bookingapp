package com.example.bookingapp.Home;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomePage {
    @Value("${application.environment}")
    private String environment;

    @GetMapping
    public String home() {
        return String.format("Cloudflows AI running in %s environment", environment);
    }
}
