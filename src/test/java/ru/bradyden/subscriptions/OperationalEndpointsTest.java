package ru.bradyden.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:operational-endpoints;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@AutoConfigureMockMvc
class OperationalEndpointsTest {
    @Autowired MockMvc mockMvc;
    @Autowired Environment environment;

    @Test
    void healthEndpointIsExposed() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiContainsOperationsAndReusableProblemResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Smart Subscription Registry API"))
                .andExpect(
                        jsonPath("$.paths['/obligations'].post.summary")
                                .value("Создать обязательство"))
                .andExpect(
                        jsonPath("$.paths['/obligations/{id}/pay'].post.responses['409']['$ref']")
                                .value("#/components/responses/Conflict"))
                .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.code").exists())
                .andExpect(
                        jsonPath(
                                        "$.components.responses.BadRequest.content['application/problem+json'].schema['$ref']")
                                .value("#/components/schemas/ApiProblem"));
    }

    @Test
    void gracefulShutdownIsEnabled() {
        assertThat(environment.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
                .isEqualTo("20s");
    }
}
