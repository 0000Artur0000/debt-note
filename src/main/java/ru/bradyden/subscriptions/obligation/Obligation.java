package ru.bradyden.subscriptions.obligation;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import ru.bradyden.subscriptions.payment.Payment;

@Entity
@Table(name = "obligations")
public class Obligation {
    @Id @UuidGenerator private UUID id;

    private String title;
    private BigDecimal amount;
    private String currency;

    @Enumerated(STRING)
    private Category category;

    @Enumerated(STRING)
    private Status status;

    @Enumerated(STRING)
    private Recurrence recurrence;

    private LocalDate nextPaymentDate;
    private Short billingAnchorDay;

    @Version private long version;

    @CreationTimestamp private Instant createdAt;

    @UpdateTimestamp private Instant updatedAt;

    protected Obligation() {}

    public static Obligation create(
            String title,
            BigDecimal amount,
            String currency,
            Category category,
            Recurrence recurrence,
            LocalDate nextPaymentDate,
            LocalDate today) {
        var obligation = new Obligation();
        obligation.title = requireText(title, "title");
        obligation.amount = requirePositive(amount);
        obligation.currency = requireText(currency, "currency");
        obligation.category = Objects.requireNonNull(category, "category must not be null");
        obligation.recurrence = recurrence;
        obligation.nextPaymentDate =
                Objects.requireNonNull(nextPaymentDate, "nextPaymentDate must not be null");
        obligation.billingAnchorDay =
                recurrence == null ? null : (short) nextPaymentDate.getDayOfMonth();
        var currentDate = Objects.requireNonNull(today, "today must not be null");
        obligation.status = nextPaymentDate.isBefore(currentDate) ? Status.EXPIRED : Status.ACTIVE;
        return obligation;
    }

    public Payment pay(Instant paidAt) {
        requireActive("pay");

        var payment = Payment.create(id, amount, currency, paidAt);

        if (recurrence == null) {
            status = Status.CANCELLED;
        } else {
            nextPaymentDate = recurrence.nextDate(nextPaymentDate, billingAnchorDay);
        }
        return payment;
    }

    public void cancel() {
        requireActive("cancel");
        status = Status.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Category getCategory() {
        return category;
    }

    public Status getStatus() {
        return status;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public Short getBillingAnchorDay() {
        return billingAnchorDay;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void requireActive(String operation) {
        if (status != Status.ACTIVE) {
            throw new InvalidObligationStateException(operation, status);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return value;
    }
}
