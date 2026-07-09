package ru.bradyden.subscriptions;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    // ТЗ использует lowercase-енумы и в query (?category=subscription), стандартная
    // конверсия MVC регистрозависима — подключаем лояльный конвертер Boot.
    @Bean
    WebMvcConfigurer lenientEnumBinding() {
        return new WebMvcConfigurer() {
            @Override
            public void addFormatters(FormatterRegistry registry) {
                ApplicationConversionService.configure(registry);
            }
        };
    }
}
