package com.aar.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Storage storage, RateLimit rateLimit) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** Thư mục lưu ảnh báo cáo, phiếu khám, ảnh hậu kiểm (báo cáo mục 5.1). */
    public record Storage(String dir, int maxFilesPerRequest) {
    }

    /**
     * Giới hạn tần suất cho /api/public/** theo địa chỉ IP, đếm theo cửa sổ 1 phút (mục 5.1).
     * Báo cứu hộ còn có giới hạn riêng theo số điện thoại (REPORT_RATE_LIMIT_PER_HOUR).
     */
    public record RateLimit(int authPerMinute, int lookupPerMinute, int reportPerMinute, int otherPerMinute) {
    }
}
