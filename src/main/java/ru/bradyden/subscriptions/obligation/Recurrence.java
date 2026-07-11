package ru.bradyden.subscriptions.obligation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public enum Recurrence {
    MONTHLY(1),
    QUARTERLY(3),
    YEARLY(12);

    private final int intervalMonths;

    Recurrence(int intervalMonths) {
        this.intervalMonths = intervalMonths;
    }

    public LocalDate nextDate(LocalDate currentDate, int billingAnchorDay) {
        Objects.requireNonNull(currentDate, "currentDate must not be null");
        if (billingAnchorDay < 1 || billingAnchorDay > 31) {
            throw new IllegalArgumentException("billingAnchorDay must be between 1 and 31");
        }

        var targetMonth = YearMonth.from(currentDate).plusMonths(intervalMonths);
        return targetMonth.atDay(Math.min(billingAnchorDay, targetMonth.lengthOfMonth()));
    }
}
