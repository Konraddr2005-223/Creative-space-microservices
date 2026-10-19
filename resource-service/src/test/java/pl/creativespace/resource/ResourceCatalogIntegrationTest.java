package pl.creativespace.resource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// WHY disabledWithoutDocker: portfolio machines (and this CI host) may lack a running Docker engine.
// When Docker is available, this remains the real Postgres + Flyway proof.
@Testcontainers(disabledWithoutDocker = true)
class ResourceCatalogIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("resource_db")
            .withUsername("resource")
            .withPassword("resource");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // WHY: tests should not depend on a running Eureka; discovery is infra, not catalog logic.
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void seedDataIsLoadedAndCrudWorks() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        String created = mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Podcast Booth B",
                                  "type": "STUDIO",
                                  "description": "Two-mic booth",
                                  "location": "Wroclaw",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Podcast Booth B"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = created.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/resources/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Podcast Booth B+",
                                  "type": "STUDIO",
                                  "description": "Upgraded booth",
                                  "location": "Wroclaw",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Podcast Booth B+"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/resources/" + id + "/price-tiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "unit": "HOUR",
                                  "amount": 75.50,
                                  "currency": "PLN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(75.50))
                .andExpect(jsonPath("$.currency").value("PLN"));

        mockMvc.perform(get("/api/resources/" + id + "/price-tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/resources")
                        .param("type", "STUDIO")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Podcast Booth B+')]").exists());
    }

    @Test
    void validationErrorsReturnProblemDetail() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "type": "STUDIO",
                                  "location": "X",
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").exists());
    }
}
