package com.pbl6.cinemate.streaming_seeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class StreamingSeederApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingSeederApplication.class, args);
    }
}
