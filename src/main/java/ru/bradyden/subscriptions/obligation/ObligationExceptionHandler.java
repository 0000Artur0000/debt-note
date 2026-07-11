package ru.bradyden.subscriptions.obligation;

import static ru.bradyden.subscriptions.common.api.ApiErrorCode.OBLIGATION_NOT_ACTIVE;
import static ru.bradyden.subscriptions.common.api.ApiErrorCode.OBLIGATION_NOT_FOUND;
import static ru.bradyden.subscriptions.common.api.ApiExceptionHandler.response;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ObligationExceptionHandler {
    @ExceptionHandler(ObligationNotFoundException.class)
    ResponseEntity<Object> handleNotFound(
            ObligationNotFoundException exception, WebRequest request) {
        return response(OBLIGATION_NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidObligationStateException.class)
    ResponseEntity<Object> handleInvalidState(
            InvalidObligationStateException exception, WebRequest request) {
        return response(OBLIGATION_NOT_ACTIVE, exception.getMessage(), request, List.of());
    }
}
