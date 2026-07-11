package ru.bradyden.subscriptions.obligation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObligationRepository extends JpaRepository<Obligation, UUID> {
    List<Obligation> findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(
            LocalDate startDate, LocalDate endDate);

    boolean existsByTitleIgnoreCaseAndStatus(String title, Status status);

    @Query(
            """
            select o from Obligation o
            where (:category is null or o.category = :category)
              and (:status is null or o.status = :status)
            order by o.nextPaymentDate
            """)
    List<Obligation> findAllFiltered(
            @Param("category") Category category, @Param("status") Status status);

    @Modifying
    @Query(
            "update Obligation o set o.status = :expired, o.updatedAt = :now "
                    + "where o.status = :active and o.nextPaymentDate < :today "
                    + "and o.recurrence is null")
    int expireOverdueOneOffs(Status active, Status expired, LocalDate today, Instant now);
}
