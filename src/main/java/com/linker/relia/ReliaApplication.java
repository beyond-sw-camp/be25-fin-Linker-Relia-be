package com.linker.relia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReliaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReliaApplication.class, args);
    }

}
