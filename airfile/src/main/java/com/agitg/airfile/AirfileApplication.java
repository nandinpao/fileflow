package com.agitg.airfile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties
@EnableScheduling
public class AirfileApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirfileApplication.class, args);
	}

}
