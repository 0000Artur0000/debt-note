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
    @Query(
            """
            select o from Obligation o
            where o.status = :status
              and o.nextPaymentDate between :startDate and :endDate
            order by o.nextPaymentDate, o.id
            """)
    List<Obligation> findUpcoming(
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByTitleIgnoreCaseAndStatus(String title, Status status);

    @Query(
            """
            select o from Obligation o
            where (:category is null or o.category = :category)
              and (:status is null or o.status = :status)
            order by o.nextPaymentDate, o.id
            """)
    List<Obligation> findAllFiltered(
            @Param("category") Category category, @Param("status") Status status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            update Obligation o
            set o.status = :expired,
                o.updatedAt = :now,
                o.version = o.version + 1
            where o.status = :active
              and o.nextPaymentDate < :today
              and o.recurrence is null
            """)
    int expireOverdueOneOffs(Status active, Status expired, LocalDate today, Instant now);
}
