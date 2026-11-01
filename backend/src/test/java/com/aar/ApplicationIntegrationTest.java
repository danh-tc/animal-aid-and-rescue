package com.aar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Chạy trên PostgreSQL thật (Testcontainers) với migration và dữ liệu mẫu.
 * Context khởi động được nghĩa là entity JPA khớp schema (ddl-auto: validate).
 * Bỏ qua tự động nếu máy không có Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class ApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void publicPetListShowsOnlyAvailablePets() throws Exception {
        mvc.perform(get("/api/public/pets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void notAvailablePetIsHiddenFromPublic() throws Exception {
        mvc.perform(get("/api/public/pets/1")).andExpect(status().isNotFound());
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        mvc.perform(post("/api/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"staff1@aar.local\",\"password\":\"sai-mat-khau\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void roleBasedAccessFollowsPermissionMatrix() throws Exception {
        mvc.perform(get("/api/staff/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/staff/dashboard").header("Authorization", bearer("adopter1@aar.local")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/dashboard").header("Authorization", bearer("staff1@aar.local")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newReports").value(2))
            .andExpect(jsonPath("$.pendingApplications").value(2));
        // Quản lý kế thừa quyền Nhân viên (RoleHierarchy)
        mvc.perform(get("/api/staff/dashboard").header("Authorization", bearer("manager@aar.local")))
            .andExpect(status().isOk());
    }

    @Test
    void registerCreatesAdopterAndMeReturnsIt() throws Exception {
        String body = "{\"email\":\"moi@aar.local\",\"password\":\"Password@123\",\"fullName\":\"Người Mới\"}";
        String response = mvc.perform(post("/api/public/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.role").value("ADOPTER"))
            .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("accessToken").asText();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("moi@aar.local"));
    }

    /** RB-01: database chặn đơn Đã duyệt thứ hai cho cùng một thú. Vàng (id 3) có hai đơn PENDING. */
    @Test
    void databaseRejectsSecondApprovedApplicationForSamePet() {
        jdbc.update("UPDATE adoption_applications SET status = 'APPROVED' WHERE id = 1");
        try {
            assertThatThrownBy(() -> jdbc.update("UPDATE adoption_applications SET status = 'APPROVED' WHERE id = 2"))
                .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.update("UPDATE adoption_applications SET status = 'PENDING' WHERE id IN (1, 2)");
        }
    }

    @Test
    void petCodeSequenceContinuesAfterSeed() {
        Long next = jdbc.queryForObject("SELECT nextval('pet_code_seq')", Long.class);
        assertThat(next).isGreaterThan(8);
    }

    private String bearer(String email) throws Exception {
        String response = mvc.perform(post("/api/public/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return "Bearer " + node.get("accessToken").asText();
    }
}
