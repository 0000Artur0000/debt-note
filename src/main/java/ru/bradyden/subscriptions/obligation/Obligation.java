package ru.bradyden.subscriptions.obligation;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import static jakarta.persistence.EnumType.STRING;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter @Setter
public class Obligation {
    @Id
    private UUID id;
    private String nazvanie;
    private BigDecimal summa;
    private String valuta;
    @Enumerated(STRING)
    private Category kategoriya;
    @Enumerated(STRING)
    private Status status;
    @Enumerated(STRING)
    private Recurrence periodichnost;
    private LocalDate dataSledPlatezha;
    private Instant sozdano;
    private Instant obnovleno;
}
