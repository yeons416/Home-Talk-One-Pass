package com.hometalk.onepass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing	// Data Auditing 사용을 위한 어노테이션
public class OnepassApplication {


    public static void main(String[] args) {
        SpringApplication.run(OnepassApplication.class, args);
    }

}





