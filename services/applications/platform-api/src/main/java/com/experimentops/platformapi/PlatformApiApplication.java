package com.experimentops.platformapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.experimentops")
public class PlatformApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
