package ru.bradyden.subscriptions.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;

@Schema(name = "ApiProblem", description = "RFC 9457 error with a stable application code")
public record ApiProblem(
        @Schema(format = "uri", example = "urn:problem:validation-failed") URI type,
        @Schema(example = "Ошибка валидации") String title,
        @Schema(example = "400") int status,
        @Schema(example = "Запрос содержит некорректные данные") String detail,
        @Schema(format = "uri", example = "/obligations") URI instance,
        @Schema(example = "validation_failed") String code,
        List<ValidationViolation> violations) {}
