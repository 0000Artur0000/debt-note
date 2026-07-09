package ru.bradyden.subscriptions.obligation;
import jakarta.persistence.Column;
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
    @Column(name = "title")
    private String nazvanie;
    @Column(name = "amount")
    private BigDecimal summa;
    @Column(name = "currency")
    private String valuta;
    @Enumerated(STRING)
    @Column(name = "category")
    private Category kategoriya;
    @Enumerated(STRING)
    private Status status;
    @Enumerated(STRING)
    @Column(name = "recurrence")
    private Recurrence periodichnost;
    @Column(name = "next_payment_date")
    private LocalDate dataSledPlatezha;
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant sozdano;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant obnovleno;
}
