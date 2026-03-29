package org.example.lyy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LyyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LyyApplication.class, args);
    }

}
