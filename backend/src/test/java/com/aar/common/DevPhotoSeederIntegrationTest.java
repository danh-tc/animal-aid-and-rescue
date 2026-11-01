package com.aar.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.aar.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Profile dev: thú trong dữ liệu mẫu có sẵn ảnh thật, xem được trên trang công khai. */
class DevPhotoSeederIntegrationTest extends IntegrationTestBase {

    @Test
    void seedPetsHavePhotosServedPublicly() throws Exception {
        // Vàng (TH-2026-0003) đang Sẵn sàng nhận nuôi, có 3 ảnh shiba
        JsonNode vang = getJson("/api/public/pets/3", null, 200);
        assertThat(vang.get("photoIds")).hasSize(3);
        long photoId = vang.get("photoIds").get(0).asLong();
        MvcResult img = mvc.perform(get("/api/public/pets/3/photos/" + photoId)).andReturn();
        assertThat(img.getResponse().getStatus()).isEqualTo(200);
        assertThat(img.getResponse().getContentType()).isEqualTo("image/jpeg");
        assertThat(img.getResponse().getContentAsByteArray().length).isGreaterThan(10_000);

        // Mực (TH-2026-0001) đang cách ly: có ảnh trong hồ sơ nội bộ nhưng không lộ ra công khai
        assertThat(getJson("/api/staff/pets/1", STAFF, 200).get("photos")).hasSize(3);
        getJson("/api/public/pets/1", null, 404);
    }
}
