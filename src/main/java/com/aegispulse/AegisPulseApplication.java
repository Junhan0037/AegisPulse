package com.aegispulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AegisPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisPulseApplication.class, args);
    }

}
