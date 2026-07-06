package ru.bradyden.subscriptions.obligation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface ObligationRepository extends JpaRepository<Obligation, UUID> {
    List<Obligation> findAllByOrderByDataSledPlatezhaAsc();
    List<Obligation> findByDataSledPlatezhaBetweenOrderByDataSledPlatezhaAsc(
        LocalDate nachalo, LocalDate konec);
    boolean existsByNazvanieIgnoreCaseAndStatus(String nazvanie, Status status);
    @Modifying
    @Query("update Obligation o set o.status = :prosrochen, o.obnovleno = :seychas "
        + "where o.status = :aktivny and o.dataSledPlatezha < :segodnya "
        + "and o.periodichnost is null")
    int pogasitProsrochennye(Status aktivny, Status prosrochen,
                              LocalDate segodnya, Instant seychas);
}
