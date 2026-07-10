package ru.bradyden.subscriptions.obligation;

import java.time.LocalDate;
import java.time.Period;

public enum Recurrence {
    MONTHLY(Period.ofMonths(1)),
    QUARTERLY(Period.ofMonths(3)),
    YEARLY(Period.ofYears(1));

    private final Period period;

    Recurrence(Period period) {
        this.period = period;
    }

    public LocalDate nextDate(LocalDate currentDate) {
        return currentDate.plus(period);
    }
}
