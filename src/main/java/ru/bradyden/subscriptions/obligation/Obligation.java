package ru.bradyden.subscriptions.obligation;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import static jakarta.persistence.EnumType.STRING;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
@Entity
@Table(name = "obligations")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class Obligation {
    @Id
    @UuidGenerator
    private UUID id;
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
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
