package ru.bradyden.subscriptions.obligation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ObligationRepository extends JpaRepository<Obligation, UUID> {
}
