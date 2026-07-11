package ru.bradyden.subscriptions.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app")
public record TimeProperties(@DefaultValue("UTC") ZoneId zoneId) {}
