package com.experimentops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.experimentops.interfaces",
        "com.experimentops.service",
        "com.experimentops.validator",
        "com.experimentops.transformer",
        "com.experimentops.objectstorage",
        "com.experimentops.common.kafka"
})
public class Scanner {
    public static void main(String[] args) {
        SpringApplication.run(Scanner.class, args);
    }
}
