package ru.bradyden.subscriptions.obligation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import ru.bradyden.subscriptions.obligation.dto.ObligationResponse;
import ru.bradyden.subscriptions.obligation.dto.PayResult;
import ru.bradyden.subscriptions.obligation.dto.PaymentResponse;
import ru.bradyden.subscriptions.obligation.dto.UpcomingResult;
import ru.bradyden.subscriptions.sse.SseBroadcaster;

@WebMvcTest(ObligationController.class)
class ObligationControllerTest {
    private static final UUID OBLIGATION_ID =
            UUID.fromString("96bdd917-3fba-4976-a52f-5c1b8b8ba419");
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 8, 9);

    @Autowired MockMvc mockMvc;

    @MockitoBean ObligationService service;
    @MockitoBean SseBroadcaster sse;

    @Test
    void createReturns201AndCurrentJsonContract() throws Exception {
        var obligation = activeSubscription(PAYMENT_DATE);
        when(service.create(any())).thenReturn(new CreateObligationResult(obligation, null));

        mockMvc.perform(
                        post("/obligations")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "title": "Яндекс.Плюс",
                                          "amount": 399.00,
                                          "currency": "RUB",
                                          "category": "subscription",
                                          "recurrence": "monthly",
                                          "next_payment_date": "2026-08-09"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.obligation.id").value(OBLIGATION_ID.toString()))
                .andExpect(jsonPath("$.obligation.title").value("Яндекс.Плюс"))
                .andExpect(jsonPath("$.obligation.category").value("subscription"))
                .andExpect(jsonPath("$.obligation.status").value("active"))
                .andExpect(jsonPath("$.obligation.recurrence").value("monthly"))
                .andExpect(jsonPath("$.obligation.next_payment_date").value("2026-08-09"))
                .andExpect(jsonPath("$.warning").doesNotExist());
    }

    @Test
    void createKeepsDuplicateAsWarningInsteadOfError() throws Exception {
        when(service.create(any()))
                .thenReturn(
                        new CreateObligationResult(
                                activeSubscription(PAYMENT_DATE),
                                "Активное обязательство с таким названием уже существует"));

        mockMvc.perform(
                        post("/obligations")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "title": "Яндекс.Плюс",
                                          "amount": 399,
                                          "currency": "RUB",
                                          "category": "subscription",
                                          "next_payment_date": "2026-08-09"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.warning")
                                .value("Активное обязательство с таким названием уже существует"));
    }

    @Test
    void createRejectsInvalidRequestBeforeCallingService() throws Exception {
        mockMvc.perform(
                        post("/obligations")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "title": " ",
                                          "amount": 0,
                                          "currency": "RUB",
                                          "category": "subscription",
                                          "next_payment_date": "2026-08-09"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void listAcceptsLowercaseCombinedFiltersAndReturnsSortedShape() throws Exception {
        when(service.list(Category.SUBSCRIPTION, Status.ACTIVE))
                .thenReturn(List.of(activeSubscription(PAYMENT_DATE)));

        mockMvc.perform(
                        get("/obligations")
                                .param("category", "subscription")
                                .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(OBLIGATION_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("active"))
                .andExpect(jsonPath("$[0].next_payment_date").value("2026-08-09"));

        verify(service).list(Category.SUBSCRIPTION, Status.ACTIVE);
    }

    @Test
    void upcomingUsesSevenDaysByDefaultAndKeepsResponseShape() throws Exception {
        var obligation = activeSubscription(PAYMENT_DATE);
        var alert =
                new UpcomingResult.RenewalAlert(
                        OBLIGATION_ID,
                        obligation.title(),
                        obligation.amount(),
                        obligation.currency(),
                        obligation.nextPaymentDate(),
                        "monthly");
        when(service.upcoming(7))
                .thenReturn(
                        new UpcomingResult(
                                List.of(obligation),
                                Map.of("RUB", new BigDecimal("399.00")),
                                List.of(alert)));

        mockMvc.perform(get("/obligations/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligations[0].title").value("Яндекс.Плюс"))
                .andExpect(jsonPath("$.totals.RUB").value(399.0))
                .andExpect(jsonPath("$.renewal_alerts[0].id").value(OBLIGATION_ID.toString()))
                .andExpect(jsonPath("$.renewal_alerts[0].recurrence").value("monthly"));

        verify(service).upcoming(7);
    }

    @Test
    void payReturnsObligationAndPayment() throws Exception {
        var obligation = activeSubscription(LocalDate.of(2026, 9, 9));
        var payment = payment();
        when(service.pay(OBLIGATION_ID)).thenReturn(new PayResult(obligation, payment));

        mockMvc.perform(post("/obligations/{id}/pay", OBLIGATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligation.status").value("active"))
                .andExpect(jsonPath("$.obligation.next_payment_date").value("2026-09-09"))
                .andExpect(jsonPath("$.payment.obligation_id").value(OBLIGATION_ID.toString()))
                .andExpect(jsonPath("$.payment.currency").value("RUB"));
    }

    @Test
    void cancelReturns204() throws Exception {
        mockMvc.perform(patch("/obligations/{id}/cancel", OBLIGATION_ID))
                .andExpect(status().isNoContent());

        verify(service).cancel(OBLIGATION_ID);
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/obligations/{id}", OBLIGATION_ID))
                .andExpect(status().isNoContent());

        verify(service).delete(OBLIGATION_ID);
    }

    @Test
    void payKeeps422ForInactiveObligation() throws Exception {
        when(service.pay(OBLIGATION_ID))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "Действие доступно только для обязательства в статусе active"));

        mockMvc.perform(post("/obligations/{id}/pay", OBLIGATION_ID))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteKeeps404ForUnknownId() throws Exception {
        doThrow(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Обязательство не найдено: " + OBLIGATION_ID))
                .when(service)
                .delete(OBLIGATION_ID);

        mockMvc.perform(delete("/obligations/{id}", OBLIGATION_ID))
                .andExpect(status().isNotFound());
    }

    private static ObligationResponse activeSubscription(LocalDate nextPaymentDate) {
        return new ObligationResponse(
                OBLIGATION_ID,
                "Яндекс.Плюс",
                new BigDecimal("399.00"),
                "RUB",
                Category.SUBSCRIPTION,
                Status.ACTIVE,
                Recurrence.MONTHLY,
                nextPaymentDate,
                null,
                null);
    }

    private static PaymentResponse payment() {
        return new PaymentResponse(
                UUID.fromString("d40b9a30-288c-4880-927c-902f9ec84d4e"),
                OBLIGATION_ID,
                new BigDecimal("399.00"),
                "RUB",
                Instant.parse("2026-07-10T12:00:00Z"));
    }
}
