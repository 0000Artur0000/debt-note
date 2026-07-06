package ru.bradyden.subscriptions.obligation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface ObligationRepository extends JpaRepository<Obligation, UUID> {
    List<Obligation> findAllByOrderByDataSledPlatezhaAsc();
    List<Obligation> findByDataSledPlatezhaBetweenOrderByDataSledPlatezhaAsc(
        LocalDate nachalo, LocalDate konec);
}
