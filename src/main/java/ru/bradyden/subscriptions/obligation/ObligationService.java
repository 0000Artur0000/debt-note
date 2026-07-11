package ru.bradyden.subscriptions.obligation;

import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.teeing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import ru.bradyden.subscriptions.obligation.dto.ObligationMapper;
import ru.bradyden.subscriptions.obligation.dto.ObligationResponse;
import ru.bradyden.subscriptions.obligation.dto.PayResult;
import ru.bradyden.subscriptions.obligation.dto.PaymentMapper;
import ru.bradyden.subscriptions.obligation.dto.UpcomingResult;
import ru.bradyden.subscriptions.payment.PaymentRepository;
import ru.bradyden.subscriptions.sse.SseBroadcaster;

@Service
public class ObligationService {
    private final ObligationRepository obligationRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;
    private final SseBroadcaster sseBroadcaster;

    public ObligationService(
            ObligationRepository obligationRepository,
            PaymentRepository paymentRepository,
            Clock clock,
            SseBroadcaster sseBroadcaster) {
        this.obligationRepository = obligationRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
        this.sseBroadcaster = sseBroadcaster;
    }

    public CreateObligationResult create(CreateObligationRequest request) {
        var today = LocalDate.now(clock);
        var duplicate =
                obligationRepository.existsByTitleIgnoreCaseAndStatus(
                        request.title(), Status.ACTIVE);

        var obligation =
                Obligation.create(
                        request.title(),
                        request.amount(),
                        request.currency(),
                        request.category(),
                        request.recurrence(),
                        request.nextPaymentDate(),
                        today);
        obligationRepository.save(obligation);

        var warning = duplicate ? "Активное обязательство с таким названием уже существует" : null;
        return new CreateObligationResult(ObligationMapper.toResponse(obligation), warning);
    }

    @Transactional
    public List<ObligationResponse> list(Category category, Status status) {
        var today = LocalDate.now(clock);
        var now = Instant.now(clock);
        obligationRepository.expireOverdueOneOffs(Status.ACTIVE, Status.EXPIRED, today, now);

        return obligationRepository.findAllFiltered(category, status).stream()
                .map(ObligationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UpcomingResult upcoming(int days) {
        var today = LocalDate.now(clock);
        var obligations =
                obligationRepository.findUpcoming(Status.ACTIVE, today, today.plusDays(days));
        var obligationResponses = obligations.stream().map(ObligationMapper::toResponse).toList();

        return obligations.stream()
                .collect(
                        teeing(
                                toMap(
                                        Obligation::getCurrency,
                                        Obligation::getAmount,
                                        BigDecimal::add,
                                        TreeMap::new),
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
                                                                        .toLowerCase(Locale.ROOT)),
                                                toList())),
                                (totals, alerts) ->
                                        new UpcomingResult(obligationResponses, totals, alerts)));
    }

    @Transactional
    public PayResult pay(UUID id) {
        var obligation = find(id);
        var payment = obligation.pay(Instant.now(clock));
        paymentRepository.save(payment);
        return new PayResult(
                ObligationMapper.toResponse(obligation), PaymentMapper.toResponse(payment));
    }

    @Transactional
    public void cancel(UUID id) {
        var obligation = find(id);
        obligation.cancel();
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
}
