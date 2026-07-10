package ru.bradyden.subscriptions.obligation.dto;

import ru.bradyden.subscriptions.obligation.Obligation;

public record CreateObligationResult(Obligation obligation, String warning) {}
