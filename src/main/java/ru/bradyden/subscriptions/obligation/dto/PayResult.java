package ru.bradyden.subscriptions.obligation.dto;
import ru.bradyden.subscriptions.obligation.Obligation;
import ru.bradyden.subscriptions.payment.Payment;
public record PayResult(Obligation obligation, Payment payment) {}
