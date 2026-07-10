package ru.bradyden.subscriptions.obligation.dto;

import ru.bradyden.subscriptions.payment.Payment;

public final class PaymentMapper {
    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getObligationId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt());
    }
}
