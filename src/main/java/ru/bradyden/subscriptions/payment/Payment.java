package ru.bradyden.subscriptions.payment;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter @Setter
public class Payment {
    @Id
    private UUID id;
}
