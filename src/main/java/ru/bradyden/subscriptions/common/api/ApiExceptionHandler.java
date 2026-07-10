package ru.bradyden.subscriptions.common.api;

import jakarta.persistence.OptimisticLockException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.bradyden.subscriptions.obligation.InvalidObligationStateException;
import ru.bradyden.subscriptions.obligation.ObligationNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ObligationNotFoundException.class)
    ResponseEntity<Object> handleObligationNotFound(
            ObligationNotFoundException exception, WebRequest request) {
        return response(
                ApiErrorCode.OBLIGATION_NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidObligationStateException.class)
    ResponseEntity<Object> handleInvalidObligationState(
            InvalidObligationStateException exception, WebRequest request) {
        return response(
                ApiErrorCode.OBLIGATION_NOT_ACTIVE, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    ResponseEntity<Object> handleConcurrentModification(
            RuntimeException exception, WebRequest request) {
        return response(
                ApiErrorCode.CONCURRENT_MODIFICATION,
                "Обязательство уже было изменено другим запросом",
                request,
                List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var violations =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(
                                error ->
                                        new ValidationViolation(
                                                error.getField(), error.getDefaultMessage()))
                        .distinct()
                        .sorted(Comparator.comparing(ValidationViolation::field))
                        .toList();
        return response(
                ApiErrorCode.VALIDATION_FAILED,
                "Запрос содержит некорректные данные",
                request,
                violations);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var violations =
                exception.getParameterValidationResults().stream()
                        .flatMap(
                                result -> {
                                    var parameterName =
                                            result.getMethodParameter().getParameterName();
                                    var field = parameterName == null ? "parameter" : parameterName;
                                    return result.getResolvableErrors().stream()
                                            .map(
                                                    error ->
                                                            new ValidationViolation(
                                                                    field,
                                                                    error.getDefaultMessage()));
                                })
                        .distinct()
                        .sorted(Comparator.comparing(ValidationViolation::field))
                        .toList();
        return response(
                ApiErrorCode.VALIDATION_FAILED,
                "Параметры запроса содержат некорректные данные",
                request,
                violations);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var field = exception.getPropertyName() == null ? "parameter" : exception.getPropertyName();
        var violations = List.of(new ValidationViolation(field, "Некорректное значение"));
        return response(
                ApiErrorCode.VALIDATION_FAILED,
                "Параметры запроса содержат некорректные данные",
                request,
                violations);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return response(
                ApiErrorCode.MALFORMED_REQUEST,
                "Тело запроса не удалось прочитать",
                request,
                List.of());
    }

    private static ResponseEntity<Object> response(
            ApiErrorCode error,
            String detail,
            WebRequest request,
            List<ValidationViolation> violations) {
        var problem = ProblemDetail.forStatusAndDetail(error.status(), detail);
        problem.setType(error.type());
        problem.setTitle(error.title());
        problem.setInstance(requestUri(request));
        problem.setProperty("code", error.code());
        if (!violations.isEmpty()) {
            problem.setProperty("violations", violations);
        }
        return ResponseEntity.status(error.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static URI requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return URI.create(servletRequest.getRequest().getRequestURI());
        }
        return URI.create("about:blank");
    }
}
