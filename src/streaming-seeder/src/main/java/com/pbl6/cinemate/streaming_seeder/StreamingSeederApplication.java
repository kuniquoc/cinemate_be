package com.pbl6.cinemate.streaming_seeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.pbl6.cinemate.streaming_seeder",
        "com.pbl6.cinemate.shared"
}, exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = {
        "com.pbl6.cinemate.streaming_seeder",
        "com.pbl6.cinemate.shared"
})
public class StreamingSeederApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingSeederApplication.class, args);
    }
}
