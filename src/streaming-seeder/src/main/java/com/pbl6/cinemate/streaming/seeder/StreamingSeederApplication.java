package com.pbl6.cinemate.streaming.seeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StreamingSeederApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingSeederApplication.class, args);
    }
}
