package ru.bradyden.subscriptions.obligation.dto;

import ru.bradyden.subscriptions.obligation.Obligation;

public final class ObligationMapper {
    private ObligationMapper() {}

    public static ObligationResponse toResponse(Obligation obligation) {
        return new ObligationResponse(
                obligation.getId(),
                obligation.getTitle(),
                obligation.getAmount(),
                obligation.getCurrency(),
                obligation.getCategory(),
                obligation.getStatus(),
                obligation.getRecurrence(),
                obligation.getNextPaymentDate(),
                obligation.getCreatedAt(),
                obligation.getUpdatedAt());
    }
}
