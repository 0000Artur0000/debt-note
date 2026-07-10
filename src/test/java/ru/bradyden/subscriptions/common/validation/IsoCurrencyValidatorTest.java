package ru.bradyden.subscriptions.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IsoCurrencyValidatorTest {
    private final IsoCurrencyValidator validator = new IsoCurrencyValidator();

    @ParameterizedTest
    @ValueSource(strings = {"RUB", "USD", "EUR", "JPY"})
    void acceptsIso4217Codes(String currency) {
        assertThat(validator.isValid(currency, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZZZ", "US", "123", "ruble"})
    void rejectsUnknownCodes(String currency) {
        assertThat(validator.isValid(currency, null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void leavesNullAndEmptyHandlingToNotBlank(String currency) {
        assertThat(validator.isValid(currency, null)).isTrue();
    }
}
