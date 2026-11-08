package com.aar.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = {"app.rate-limit.auth-per-minute=3", "app.rate-limit.lookup-per-minute=2"})
class RateLimitIntegrationTest extends IntegrationTestBase {

    @Test
    void loginIsLimitedPerIp() throws Exception {
        String body = "{\"email\":\"khongco@aar.local\",\"password\":\"sai-mat-khau\"}";
        int limited = 0;
        MvcResult last = null;
        for (int i = 0; i < 5; i++) {
            last = mvc.perform(post("/api/public/auth/login").with(r -> { r.setRemoteAddr("10.0.0.7"); return r; })
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
            if (last.getResponse().getStatus() == 429) {
                limited++;
            }
        }
        assertThat(limited).isEqualTo(2);
        assertThat(last.getResponse().getHeader("Retry-After")).isNotBlank();
        JsonNode error = json.readTree(last.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(error.get("code").asText()).isEqualTo("RATE_LIMITED");

        int other = mvc.perform(post("/api/public/auth/login").with(r -> { r.setRemoteAddr("10.0.0.8"); return r; })
            .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
        assertThat(other).isEqualTo(401);
    }

    @Test
    void lookupHasOwnLimitAndPetListStaysOpen() throws Exception {
        int[] statuses = new int[3];
        for (int i = 0; i < 3; i++) {
            statuses[i] = mvc.perform(get("/api/public/rescue-reports/lookup?code=BC-AAAAAA&phone=0900000000")
                .with(r -> { r.setRemoteAddr("10.0.0.9"); return r; })).andReturn().getResponse().getStatus();
        }
        assertThat(statuses).containsExactly(404, 404, 429);
        int pets = mvc.perform(get("/api/public/pets").with(r -> { r.setRemoteAddr("10.0.0.9"); return r; }))
            .andReturn().getResponse().getStatus();
        assertThat(pets).isEqualTo(200);
    }
}
