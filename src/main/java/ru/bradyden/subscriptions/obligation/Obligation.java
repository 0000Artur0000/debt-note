package ru.bradyden.subscriptions.obligation;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter @Setter
public class Obligation {
    @Id
    private UUID id;
    private String nazvanie;
}
