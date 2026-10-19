package pl.creativespace.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.creativespace.booking.dto.CreateBookingRequest;
import pl.creativespace.booking.service.BookingService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portfolio-critical proof: EXCLUDE beats the race that optimistic locking cannot catch on inserts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class BookingOverlapIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("booking_db")
            .withUsername("booking")
            .withPassword("booking");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        // WHY: overlap tests do not need Kafka; disable poller so context stays lightweight.
        registry.add("app.outbox.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BookingService bookingService;

    @Test
    void sequentialOverlapReturnsConflictAndCancelFreesSlot() throws Exception {
        UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        String first = """
                {
                  "resourceId": "%s",
                  "userId": "alice",
                  "startAt": "2026-06-01T10:00:00Z",
                  "endAt": "2026-06-01T12:00:00Z"
                }
                """.formatted(resourceId);

        MvcResult created = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String bookingId = created.getResponse().getContentAsString()
                .replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        String overlapping = """
                {
                  "resourceId": "%s",
                  "userId": "bob",
                  "startAt": "2026-06-01T11:00:00Z",
                  "endAt": "2026-06-01T13:00:00Z"
                }
                """.formatted(resourceId);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlapping))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Resource already booked for an overlapping time range"));

        // Back-to-back (half-open) must succeed: [10,12) and [12,14)
        String adjacent = """
                {
                  "resourceId": "%s",
                  "userId": "carol",
                  "startAt": "2026-06-01T12:00:00Z",
                  "endAt": "2026-06-01T14:00:00Z"
                }
                """.formatted(resourceId);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjacent))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first.replace("alice", "dave")))
                .andExpect(status().isCreated());
    }

    @Test
    void fiftyParallelRequestsYieldExactlyOneSuccess() throws Exception {
        // WHY call the service (not MockMvc): MockMvc is not thread-safe; EXCLUDE is proven at the DB/session layer.
        UUID resourceId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Instant start = Instant.parse("2026-07-01T10:00:00Z");
        Instant end = Instant.parse("2026-07-01T11:00:00Z");

        int parallelism = 50;
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Void>> tasks = new ArrayList<>(parallelism);
            for (int i = 0; i < parallelism; i++) {
                int idx = i;
                tasks.add(() -> {
                    try {
                        bookingService.create(new CreateBookingRequest(
                                resourceId, "user-" + idx, start, end
                        ));
                        successes.incrementAndGet();
                    } catch (ResponseStatusException ex) {
                        if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                            conflicts.incrementAndGet();
                        } else {
                            throw ex;
                        }
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        }

        assertThat(successes.get()).as("exactly one winner under EXCLUDE").isEqualTo(1);
        assertThat(conflicts.get()).as("everyone else gets 409").isEqualTo(parallelism - 1);

        mockMvc.perform(get("/api/bookings")
                        .param("resourceId", resourceId.toString())
                        .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
