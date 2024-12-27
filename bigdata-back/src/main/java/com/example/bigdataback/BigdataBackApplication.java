package com.example.bigdataback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.example.bigdataback"})
@EnableMongoRepositories(basePackages = "com.example.bigdataback.repository")
public class BigdataBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigdataBackApplication.class, args);
    }
}
