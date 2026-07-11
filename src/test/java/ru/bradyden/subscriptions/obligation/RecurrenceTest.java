package ru.bradyden.subscriptions.obligation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecurrenceTest {
    @ParameterizedTest
    @CsvSource({
        "MONTHLY,2025-01-31,31,2025-02-28",
        "MONTHLY,2024-01-31,31,2024-02-29",
        "MONTHLY,2025-02-28,31,2025-03-31",
        "QUARTERLY,2025-11-30,31,2026-02-28",
        "QUARTERLY,2026-02-28,31,2026-05-31",
        "YEARLY,2024-02-29,29,2025-02-28",
        "YEARLY,2027-02-28,29,2028-02-29"
    })
    void calculatesDateFromOriginalBillingAnchor(
            Recurrence recurrence,
            LocalDate currentDate,
            int billingAnchorDay,
            LocalDate expectedDate) {
        assertThat(recurrence.nextDate(currentDate, billingAnchorDay)).isEqualTo(expectedDate);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 32})
    void rejectsInvalidBillingAnchor(int billingAnchorDay) {
        assertThatThrownBy(
                        () ->
                                Recurrence.MONTHLY.nextDate(
                                        LocalDate.of(2026, 1, 1), billingAnchorDay))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("billingAnchorDay");
    }
}
