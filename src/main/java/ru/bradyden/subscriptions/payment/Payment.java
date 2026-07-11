package ru.bradyden.subscriptions.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @UuidGenerator private UUID id;

    private UUID obligationId;
    private BigDecimal amount;
    private String currency;
    private Instant paidAt;

    protected Payment() {}

    public static Payment create(
            UUID obligationId, BigDecimal amount, String currency, Instant paidAt) {
        var payment = new Payment();
        payment.obligationId =
                Objects.requireNonNull(obligationId, "obligationId must not be null");
        payment.amount = Objects.requireNonNull(amount, "amount must not be null");
        payment.currency = Objects.requireNonNull(currency, "currency must not be null");
        payment.paidAt = Objects.requireNonNull(paidAt, "paidAt must not be null");
        return payment;
    }

    public UUID getId() {
        return id;
    }

    public UUID getObligationId() {
        return obligationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
