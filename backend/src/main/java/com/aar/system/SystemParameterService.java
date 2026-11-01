package com.aar.system;

import com.aar.common.error.BusinessException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemParameterService {

    public static final String MIN_ADOPTION_AGE_WEEKS = "MIN_ADOPTION_AGE_WEEKS";
    public static final String MIN_STERILIZATION_AGE_WEEKS = "MIN_STERILIZATION_AGE_WEEKS";
    public static final String QUARANTINE_DAYS = "QUARANTINE_DAYS";
    public static final String FOLLOW_UP_1_DAYS = "FOLLOW_UP_1_DAYS";
    public static final String FOLLOW_UP_2_DAYS = "FOLLOW_UP_2_DAYS";
    public static final String REPORT_RATE_LIMIT_PER_HOUR = "REPORT_RATE_LIMIT_PER_HOUR";
    public static final String APPLICATION_ALERT_DAYS = "APPLICATION_ALERT_DAYS";

    private final SystemParameterRepository params;

    public SystemParameterService(SystemParameterRepository params) {
        this.params = params;
    }

    @Transactional(readOnly = true)
    public int getInt(String key) {
        SystemParameter p = params.findById(key)
            .orElseThrow(() -> new IllegalStateException("Thiếu tham số hệ thống " + key));
        return Integer.parseInt(p.getValue());
    }

    @Transactional(readOnly = true)
    public List<SystemParameter> all() {
        return params.findAll(Sort.by("key"));
    }

    /** Mọi tham số hiện có đều là số nguyên dương. */
    @Transactional
    public SystemParameter update(String key, String value) {
        SystemParameter p = params.findById(key)
            .orElseThrow(() -> BusinessException.notFound("Không có tham số " + key));
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("INVALID_PARAMETER", "Giá trị phải là số nguyên");
        }
        if (parsed <= 0 || parsed > 10_000) {
            throw BusinessException.badRequest("INVALID_PARAMETER", "Giá trị phải từ 1 đến 10000");
        }
        p.changeValue(Integer.toString(parsed));
        return p;
    }
}
