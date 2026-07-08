package ru.bradyden.subscriptions.obligation;
import java.time.LocalDate;
import java.time.Period;
public enum Recurrence {
    MONTHLY(Period.ofMonths(1)),
    QUARTERLY(Period.ofMonths(3)),
    YEARLY(Period.ofYears(1));
    private final Period shag;
    Recurrence(Period shag) { this.shag = shag; }
    public LocalDate sleduyushchaya(LocalDate ot) { return ot.plus(shag); }
}
