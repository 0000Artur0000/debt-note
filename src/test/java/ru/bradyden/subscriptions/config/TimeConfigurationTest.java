package ru.bradyden.subscriptions.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TimeConfigurationTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TimeConfiguration.class);

    @Test
    void usesUtcByDefault() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("UTC"));
                });
    }

    @Test
    void usesConfiguredZone() {
        contextRunner
                .withPropertyValues("app.zone-id=Asia/Yekaterinburg")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBean(Clock.class).getZone())
                                    .isEqualTo(ZoneId.of("Asia/Yekaterinburg"));
                        });
    }

    @Test
    void rejectsUnknownZone() {
        contextRunner
                .withPropertyValues("app.zone-id=not-a-time-zone")
                .run(context -> assertThat(context).hasFailed());
    }
}
