package com.pbl6.cinemate.streaming_signaling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@ConfigurationPropertiesScan
public class StreamingSignalingApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingSignalingApplication.class, args);
    }
}
