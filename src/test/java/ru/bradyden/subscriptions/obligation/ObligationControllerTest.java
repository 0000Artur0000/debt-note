package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.sse.SseBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(ObligationController.class)
class ObligationControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ObligationService servis;
    @MockitoBean SseBroadcaster sse;
    @Test void sozdatVozvrashchaet201() throws Exception {
        var o=new Obligation();o.setTitle("test");o.setStatus(Status.ACTIVE);
        when(servis.sozdat(any())).thenReturn(new CreateObligationResult(o,null));
        mockMvc.perform(post("/obligations").contentType("application/json")
            .content("{\"title\":\"test\",\"amount\":100,\"currency\":\"RUB\",\"category\":\"subscription\",\"next_payment_date\":\"2026-08-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.obligation.title").value("test"))
            .andExpect(jsonPath("$.obligation.status").value("active"));
    }
}
