package com.pbl6.cinemate.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
        "com.pbl6.cinemate.movie",
        "com.pbl6.cinemate.shared"
})
@EnableScheduling
@EnableAsync
@EnableFeignClients(basePackages = "com.pbl6.cinemate.movie.client")
@ConfigurationPropertiesScan(basePackages = {
        "com.pbl6.cinemate.movie",
        "com.pbl6.cinemate.shared"
})
public class MovieApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        SpringApplication.run(MovieApplication.class, args);
    }

}
