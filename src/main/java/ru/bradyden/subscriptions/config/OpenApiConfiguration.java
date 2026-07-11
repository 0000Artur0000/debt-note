package ru.bradyden.subscriptions.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import ru.bradyden.subscriptions.common.api.ApiProblem;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    private static final String PROBLEM_SCHEMA = "#/components/schemas/ApiProblem";

    @Bean
    OpenAPI subscriptionsOpenApi() {
        var components = new Components();
        ModelConverters.getInstance().read(ApiProblem.class).forEach(components::addSchemas);
        components
                .addResponses("BadRequest", problemResponse("Некорректный запрос"))
                .addResponses("NotFound", problemResponse("Обязательство не найдено"))
                .addResponses("Conflict", problemResponse("Конкурентное изменение"))
                .addResponses("UnprocessableEntity", problemResponse("Недопустимое состояние"));

        return new OpenAPI()
                .info(new Info().title("Smart Subscription Registry API").version("v1"))
                .components(components);
    }

    private static ApiResponse problemResponse(String description) {
        var mediaType =
                new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref(PROBLEM_SCHEMA));
        return new ApiResponse()
                .description(description)
                .content(
                        new Content()
                                .addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE, mediaType));
    }
}
