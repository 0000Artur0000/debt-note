package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.sse.SseBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(ObligationController.class)
class ObligationControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean ObligationService servis;
    @MockBean SseBroadcaster sse;
    @Test void sozdatVozvrashchaet201() throws Exception {
        var o=new Obligation();o.setNazvanie("test");o.setStatus(Status.ACTIVE);
        when(servis.sozdat(any())).thenReturn(new CreateObligationResult(o,null));
        mockMvc.perform(post("/obligations").contentType("application/json")
            .content("{\"nazvanie\":\"test\",\"summa\":100,\"valuta\":\"RUB\",\"kategoriya\":\"subscription\",\"dataSledPlatezha\":\"2026-08-01\"}"))
            .andExpect(status().isCreated());
    }
}
