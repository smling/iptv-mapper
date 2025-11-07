package io.github.smling.iptv_mapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IptvMapperApplication {
	public static void main(String[] args) {
        SpringApplication.run(IptvMapperApplication.class, args);
	}
}
