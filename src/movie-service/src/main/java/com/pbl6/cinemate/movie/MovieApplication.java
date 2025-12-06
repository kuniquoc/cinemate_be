package com.pbl6.cinemate.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = { "com.pbl6.cinemate.movie", "com.pbl6.cinemate.shared" })
public class MovieApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

		SpringApplication.run(MovieApplication.class, args);
	}

}
