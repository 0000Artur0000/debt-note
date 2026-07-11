package ru.bradyden.subscriptions.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TimeProperties.class)
public class TimeConfiguration {
    @Bean
    Clock clock(TimeProperties properties) {
        return Clock.system(properties.zoneId());
    }
}
