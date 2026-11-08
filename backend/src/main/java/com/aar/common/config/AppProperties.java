package com.aar.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Storage storage, RateLimit rateLimit) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String dir, int maxFilesPerRequest) {
    }

    public record RateLimit(int authPerMinute, int lookupPerMinute, int reportPerMinute, int otherPerMinute) {
    }
}
