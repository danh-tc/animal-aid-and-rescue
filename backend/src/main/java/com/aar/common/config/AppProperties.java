package com.aar.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Storage storage) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** Thư mục lưu ảnh báo cáo, phiếu khám, ảnh hậu kiểm (báo cáo mục 5.1). */
    public record Storage(String dir, int maxFilesPerRequest) {
    }
}
