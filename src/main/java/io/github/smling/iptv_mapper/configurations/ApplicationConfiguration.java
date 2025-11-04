package io.github.smling.iptv_mapper.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
