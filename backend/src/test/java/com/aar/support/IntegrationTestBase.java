package com.aar.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest

@TestPropertySource(properties = {
    "app.rate-limit.auth-per-minute=100000",
    "app.rate-limit.lookup-per-minute=100000",
    "app.rate-limit.report-per-minute=100000",
    "app.rate-limit.other-per-minute=100000"})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    public static final String STAFF = "staff1@aar.local";
    public static final String STAFF2 = "staff2@aar.local";
    public static final String MANAGER = "manager@aar.local";
    public static final String ADOPTER1 = "adopter1@aar.local";
    public static final String ADOPTER2 = "adopter2@aar.local";
    public static final String ADOPTER3 = "adopter3@aar.local";
    public static final long STAFF_ID = 2L;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws Exception {
        Path dir = Files.createTempDirectory("aar-uploads");
        registry.add("app.storage.dir", dir::toString);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    @org.junit.jupiter.api.BeforeEach
    void relaxPendingLimit() {
        jdbc.update("UPDATE system_parameters SET param_value = '1000' WHERE param_key = 'MAX_PENDING_APPLICATIONS'");
    }

    protected String token(String email) throws Exception {
        String cached = TOKENS.get(email);
        if (cached != null) {
            return cached;
        }
        MvcResult res = mvc.perform(post("/api/public/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}")).andReturn();
        if (res.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Đăng nhập thất bại " + email + ": " + res.getResponse().getContentAsString());
        }
        String token = json.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
        TOKENS.put(email, token);
        return token;
    }

    protected JsonNode call(MockHttpServletRequestBuilder req, String as, int expectedStatus) throws Exception {
        if (as != null) {
            req.header("Authorization", "Bearer " + token(as));
        }
        MvcResult res = mvc.perform(req).andReturn();
        String body = res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        if (res.getResponse().getStatus() != expectedStatus) {
            throw new AssertionError("Mong đợi HTTP " + expectedStatus + " nhưng nhận " + res.getResponse().getStatus()
                + " cho " + res.getRequest().getMethod() + " " + res.getRequest().getRequestURI() + ": " + body);
        }
        return body.isEmpty() ? null : json.readTree(body);
    }

    protected JsonNode postJson(String url, String as, Object body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder req = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            req.content(json.writeValueAsString(body));
        }
        return call(req, as, expectedStatus);
    }

    protected JsonNode putJson(String url, String as, Object body, int expectedStatus) throws Exception {
        return call(put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), as,
            expectedStatus);
    }

    protected JsonNode patchJson(String url, String as, Object body, int expectedStatus) throws Exception {
        return call(patch(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), as,
            expectedStatus);
    }

    protected JsonNode getJson(String url, String as, int expectedStatus) throws Exception {
        return call(get(url), as, expectedStatus);
    }

    protected static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    protected static String randomPhone() {
        return "09" + String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000L));
    }

    protected String submitReport() throws Exception {
        return postJson("/api/public/rescue-reports", null, map(
            "reporterName", "Người báo", "reporterPhone", randomPhone(),
            "locationText", "Hẻm 10, Quận 3", "description", "Chó con đi lạc"), 201).get("code").asText();
    }

    protected long reportId(String code) {
        return jdbc.queryForObject("SELECT id FROM rescue_reports WHERE code = ?", Long.class, code);
    }

    protected long rescuedPet(String species, LocalDate birth) throws Exception {
        long reportId = reportId(submitReport());
        JsonNode cases = postJson("/api/staff/rescue-reports/" + reportId + "/cases", MANAGER,
            map("numberOfAnimals", 1, "assignedStaffId", STAFF_ID), 201);
        long caseId = cases.get(0).get("id").asLong();
        JsonNode result = postJson("/api/staff/rescue-cases/" + caseId + "/succeed", STAFF, map(
            "pet", map("name", "Thú test", "species", species, "sex", "MALE", "estimatedBirthDate", birth.toString()),
            "intake", map("priority", "STABLE", "reason", "Đi lạc")), 200);
        return result.get("petId").asLong();
    }

    protected long availablePet(boolean young) throws Exception {
        LocalDate birth = LocalDate.now().minusDays(young ? 100 : 400);
        long petId = rescuedPet("DOG", birth);
        JsonNode vet = postJson("/api/staff/pets/" + petId + "/vet-requests", STAFF,
            map("clinicId", 1, "reason", "Khám tổng quát", "priority", "STABLE", "careMode", "OUTPATIENT"), 201);
        postJson("/api/staff/vet-requests/" + vet.get("request").get("id").asLong() + "/complete", STAFF,
            map("diagnosisSummary", "Khỏe", "totalCost", 500000, "healthOk", true), 200);
        backdateIntakes(petId, 30);
        JsonNode result = putJson("/api/staff/pets/" + petId + "/eligibility", STAFF,
            map("vaccinated", true, "dewormed", true, "sterilized", !young, "temperamentOk", true), 200);
        if (!"AVAILABLE".equals(result.get("status").asText())) {
            throw new IllegalStateException("Thú chưa Sẵn sàng: " + result);
        }
        return petId;
    }

    protected void backdateIntakes(long petId, int days) {
        jdbc.update("UPDATE intakes SET intake_at = now() - make_interval(days => ?) WHERE pet_id = ?", days, petId);
    }

    protected String petStatus(long petId) {
        return jdbc.queryForObject("SELECT status FROM pets WHERE id = ?", String.class, petId);
    }

    protected String applicationStatus(long applicationId) {
        return jdbc.queryForObject("SELECT status FROM adoption_applications WHERE id = ?", String.class, applicationId);
    }

    protected long submitApplication(long petId, String adopter) throws Exception {
        return postJson("/api/adopter/applications", adopter, map("petId", petId, "housingType", "HOUSE",
            "hasPetExperience", true, "motivation", "Yêu chó"), 201).get("id").asLong();
    }

    protected long approvedApplication(String adopter) throws Exception {
        long petId = availablePet(false);
        long appId = submitApplication(petId, adopter);
        postJson("/api/staff/applications/" + appId + "/approve", STAFF, null, 200);
        return appId;
    }

    protected JsonNode handedOver(String adopter) throws Exception {
        return postJson("/api/staff/applications/" + approvedApplication(adopter) + "/handover", STAFF, null, 201);
    }

    protected long petIdOfApplication(long applicationId) {
        return jdbc.queryForObject("SELECT pet_id FROM adoption_applications WHERE id = ?", Long.class, applicationId);
    }

    protected String newAdopter(String phone) throws Exception {
        String email = "adopter-" + UUID.randomUUID() + "@test.local";
        postJson("/api/public/auth/register", null,
            map("email", email, "password", "Password@123", "fullName", "Người thử", "phone", phone), 201);
        return email;
    }
}
