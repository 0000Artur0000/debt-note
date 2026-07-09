package ru.bradyden.subscriptions.obligation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface ObligationRepository extends JpaRepository<Obligation, UUID> {
    List<Obligation> findAllByOrderByNextPaymentDateAsc();
    List<Obligation> findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(
        LocalDate nachalo, LocalDate konec);
    boolean existsByTitleIgnoreCaseAndStatus(String title, Status status);
    @Modifying
    @Query("update Obligation o set o.status = :prosrochen, o.updatedAt = :seychas "
        + "where o.status = :aktivny and o.nextPaymentDate < :segodnya "
        + "and o.recurrence is null")
    int pogasitProsrochennye(Status aktivny, Status prosrochen,
                              LocalDate segodnya, Instant seychas);
}
