package com.KIRA_ZINA.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GamesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamesBackendApplication.class, args);
    }
}
