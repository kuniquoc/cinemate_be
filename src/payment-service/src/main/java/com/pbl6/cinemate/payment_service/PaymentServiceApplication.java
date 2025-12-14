package com.pbl6.cinemate.payment_service;

import java.util.TimeZone;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"com.pbl6.cinemate.payment_service",
		"com.pbl6.cinemate.shared"
})
@EnableScheduling
@EnableFeignClients
@ComponentScan(basePackages = {
	"com.pbl6.cinemate.payment_service",
	"com.pbl6.cinemate.shared"
})
@ConfigurationPropertiesScan(basePackages = {
		"com.pbl6.cinemate.payment_service",
		"com.pbl6.cinemate.shared"
})
public class PaymentServiceApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		// Configure ModelMapper settings if needed
		modelMapper.getConfiguration()
				.setSkipNullEnabled(true)
				.setAmbiguityIgnored(true);
		return modelMapper;
	}

}
