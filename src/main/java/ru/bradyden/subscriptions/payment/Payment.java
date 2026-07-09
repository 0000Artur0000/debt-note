package ru.bradyden.subscriptions.payment;
import jakarta.persistence.Column;
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
    @Column(name = "obligation_id")
    private UUID idObyazatelstva;
    @Column(name = "amount")
    private BigDecimal summa;
    @Column(name = "currency")
    private String valuta;
    @Column(name = "paid_at")
    private Instant oplacheno;
}
