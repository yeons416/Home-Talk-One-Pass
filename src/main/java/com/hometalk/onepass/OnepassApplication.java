package com.hometalk.onepass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class OnepassApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnepassApplication.class, args);
    }

}
