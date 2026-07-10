package ru.bradyden.subscriptions.obligation;

import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.teeing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import ru.bradyden.subscriptions.obligation.dto.ObligationMapper;
import ru.bradyden.subscriptions.obligation.dto.ObligationResponse;
import ru.bradyden.subscriptions.obligation.dto.PayResult;
import ru.bradyden.subscriptions.obligation.dto.PaymentMapper;
import ru.bradyden.subscriptions.obligation.dto.UpcomingResult;
import ru.bradyden.subscriptions.payment.Payment;
import ru.bradyden.subscriptions.sse.SseBroadcaster;

@Service
public class ObligationService {
    private final ObligationRepository obligationRepository;
    private final EntityManager entityManager;
    private final Clock clock;
    private final SseBroadcaster sseBroadcaster;

    public ObligationService(
            ObligationRepository obligationRepository,
            EntityManager entityManager,
            Clock clock,
            SseBroadcaster sseBroadcaster) {
        this.obligationRepository = obligationRepository;
        this.entityManager = entityManager;
        this.clock = clock;
        this.sseBroadcaster = sseBroadcaster;
    }

    public CreateObligationResult create(CreateObligationRequest request) {
        var today = LocalDate.now(clock);
        var duplicate =
                obligationRepository.existsByTitleIgnoreCaseAndStatus(
                        request.title(), Status.ACTIVE);

        var obligation = new Obligation();
        obligation.setTitle(request.title());
        obligation.setAmount(request.amount());
        obligation.setCurrency(request.currency());
        obligation.setCategory(request.category());
        obligation.setRecurrence(request.recurrence());
        obligation.setNextPaymentDate(request.nextPaymentDate());
        obligation.setStatus(
                request.nextPaymentDate().isBefore(today) ? Status.EXPIRED : Status.ACTIVE);
        obligationRepository.save(obligation);

        var warning = duplicate ? "Активное обязательство с таким названием уже существует" : null;
        return new CreateObligationResult(ObligationMapper.toResponse(obligation), warning);
    }

    @Transactional
    public List<ObligationResponse> list(Category category, Status status) {
        var today = LocalDate.now(clock);
        var now = Instant.now(clock);
        obligationRepository.expireOverdueOneOffs(Status.ACTIVE, Status.EXPIRED, today, now);

        var probe = new Obligation();
        probe.setCategory(category);
        probe.setStatus(status);
        return obligationRepository.findAll(Example.of(probe), Sort.by("nextPaymentDate")).stream()
                .map(ObligationMapper::toResponse)
                .toList();
    }

    public UpcomingResult upcoming(int days) {
        var today = LocalDate.now(clock);
        var obligations =
                obligationRepository.findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                        today, today.plusDays(days));
        var obligationResponses = obligations.stream().map(ObligationMapper::toResponse).toList();

        return obligations.stream()
                .collect(
                        teeing(
                                toMap(
                                        Obligation::getCurrency,
                                        Obligation::getAmount,
                                        BigDecimal::add),
                                filtering(
                                        obligation ->
                                                obligation.getCategory() == Category.SUBSCRIPTION
                                                        && obligation.getRecurrence() != null,
                                        mapping(
                                                obligation ->
                                                        new UpcomingResult.RenewalAlert(
                                                                obligation.getId(),
                                                                obligation.getTitle(),
                                                                obligation.getAmount(),
                                                                obligation.getCurrency(),
                                                                obligation.getNextPaymentDate(),
                                                                obligation
                                                                        .getRecurrence()
                                                                        .name()
                                                                        .toLowerCase()),
                                                toList())),
                                (totals, alerts) ->
                                        new UpcomingResult(obligationResponses, totals, alerts)));
    }

    @Transactional
    public PayResult pay(UUID id) {
        var obligation = find(id);
        requireActive(obligation, "pay");

        var payment = new Payment();
        payment.setObligationId(obligation.getId());
        payment.setAmount(obligation.getAmount());
        payment.setCurrency(obligation.getCurrency());
        payment.setPaidAt(Instant.now(clock));
        entityManager.persist(payment);

        if (obligation.getRecurrence() == null) {
            obligation.setStatus(Status.CANCELLED);
        } else {
            obligation.setNextPaymentDate(
                    obligation.getRecurrence().nextDate(obligation.getNextPaymentDate()));
        }
        return new PayResult(
                ObligationMapper.toResponse(obligation), PaymentMapper.toResponse(payment));
    }

    @Transactional
    public void cancel(UUID id) {
        var obligation = find(id);
        requireActive(obligation, "cancel");
        obligation.setStatus(Status.CANCELLED);
    }

    @Transactional
    public void delete(UUID id) {
        if (!obligationRepository.existsById(id)) {
            throw new ObligationNotFoundException(id);
        }
        obligationRepository.deleteById(id);
        sseBroadcaster.broadcast(new ObligationDeleted(id));
    }

    private Obligation find(UUID id) {
        return obligationRepository
                .findById(id)
                .orElseThrow(() -> new ObligationNotFoundException(id));
    }

    private static void requireActive(Obligation obligation, String operation) {
        if (obligation.getStatus() != Status.ACTIVE) {
            throw new InvalidObligationStateException(operation, obligation.getStatus());
        }
    }
}
