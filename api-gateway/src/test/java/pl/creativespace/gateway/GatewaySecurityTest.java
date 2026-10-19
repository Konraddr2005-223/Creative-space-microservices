package pl.creativespace.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GatewaySecurityTest.TestJwtConfig.class)
@TestPropertySource(properties = {
        // WHY: tests must not call a live Keycloak for issuer discovery.
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class GatewaySecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRouteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/resource-service/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithJwtIsNotUnauthorized() throws Exception {
        // 404/503 is fine: Eureka/resource-service may be down; auth must have passed (not 401).
        mockMvc.perform(get("/resource-service/api/resources").with(jwt().jwt(j -> j.subject("demo"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .headers(h -> h.put("alg", "none"))
                    .claims(c -> c.putAll(Map.of(
                            "sub", "test-user",
                            "iss", "http://localhost:8180/realms/creative-space"
                    )))
                    .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .expiresAt(Instant.parse("2099-01-01T00:00:00Z"))
                    .build();
        }
    }
}
