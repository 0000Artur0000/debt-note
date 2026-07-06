package ru.bradyden.subscriptions.payment;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter @Setter
public class Payment {
    @Id
    private UUID id;
    private UUID idObyazatelstva;
    private BigDecimal summa;
    private String valuta;
    private Instant oplacheno;
}
