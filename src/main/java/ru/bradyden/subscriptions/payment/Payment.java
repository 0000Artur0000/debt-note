package ru.bradyden.subscriptions.payment;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
@Entity
@Table(name = "payments")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {
    @Id
    @UuidGenerator
    private UUID id;
    private UUID obligationId;
    private BigDecimal amount;
    private String currency;
    private Instant paidAt;
}
