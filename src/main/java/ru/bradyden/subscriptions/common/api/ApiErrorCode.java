package ru.bradyden.subscriptions.common.api;

import java.net.URI;
import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "malformed_request", "Некорректный формат запроса"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "validation_failed", "Ошибка валидации"),
    OBLIGATION_NOT_FOUND(HttpStatus.NOT_FOUND, "obligation_not_found", "Обязательство не найдено"),
    CONCURRENT_MODIFICATION(
            HttpStatus.CONFLICT, "concurrent_modification", "Конфликт изменения данных"),
    OBLIGATION_NOT_ACTIVE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "obligation_not_active",
            "Операция недоступна для текущего статуса");

    private final HttpStatus status;
    private final String code;
    private final String title;

    ApiErrorCode(HttpStatus status, String code, String title) {
        this.status = status;
        this.code = code;
        this.title = title;
    }

    HttpStatus status() {
        return status;
    }

    String code() {
        return code;
    }

    String title() {
        return title;
    }

    URI type() {
        return URI.create("urn:problem:" + code.replace('_', '-'));
    }
}
