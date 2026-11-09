package com.aar.common.security;

import com.aar.common.config.AppProperties;
import com.aar.common.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final AppProperties.RateLimit limits;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public PublicRateLimitFilter(AppProperties props, ObjectMapper objectMapper) {
        this(props, objectMapper, Clock.systemUTC());
    }

    PublicRateLimitFilter(AppProperties props, ObjectMapper objectMapper, Clock clock) {
        this.limits = props.rateLimit();
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/public/")
            || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String bucket = bucketOf(request);
        int limit = limitOf(bucket);
        long now = clock.millis();
        long windowStart = now - now % WINDOW_MILLIS;
        String key = bucket + "|" + request.getRemoteAddr();

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> w.start < windowStart);
        }
        Window window = windows.compute(key, (k, w) -> w == null || w.start != windowStart ? new Window(windowStart) : w);
        if (window.count.incrementAndGet() > limit) {
            long retryAfter = Math.max(1, (windowStart + WINDOW_MILLIS - now + 999) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(),
                ApiError.of("RATE_LIMITED", "Bạn thao tác quá nhanh, vui lòng thử lại sau " + retryAfter + " giây"));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String bucketOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean post = HttpMethod.POST.matches(request.getMethod());
        if (uri.startsWith("/api/public/auth/")) {
            return "auth";
        }
        if (uri.startsWith("/api/public/rescue-reports/lookup")) {
            return "lookup";
        }
        if (post && uri.startsWith("/api/public/rescue-reports")) {
            return "report";
        }
        return "other";
    }

    private int limitOf(String bucket) {
        return switch (bucket) {
            case "auth" -> limits.authPerMinute();
            case "lookup" -> limits.lookupPerMinute();
            case "report" -> limits.reportPerMinute();
            default -> limits.otherPerMinute();
        };
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();

        Window(long start) {
            this.start = start;
        }
    }
}
