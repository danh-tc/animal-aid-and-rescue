package com.aar.common.code;

import java.security.SecureRandom;
import java.time.Year;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sinh mã nghiệp vụ (báo cáo mục 4.2).
 * <ul>
 *   <li>Mã báo cáo: BC- + 6 ký tự ngẫu nhiên, bỏ các ký tự dễ nhầm (0, O, 1, I, L). Khó đoán, dùng để tra cứu.</li>
 *   <li>Mã hồ sơ thú: TH-YYYY-NNNN, NNNN lấy từ sequence pet_code_seq, tăng liên tục không đặt lại theo năm.</li>
 * </ul>
 */
@Component
public class CodeGenerator {

    static final String REPORT_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    static final int REPORT_RANDOM_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();
    private final JdbcTemplate jdbc;

    public CodeGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String nextReportCode() {
        return reportCode(random);
    }

    public String nextPetCode() {
        Long seq = jdbc.queryForObject("SELECT nextval('pet_code_seq')", Long.class);
        return petCode(Year.now().getValue(), seq);
    }

    static String reportCode(SecureRandom random) {
        StringBuilder sb = new StringBuilder("BC-");
        for (int i = 0; i < REPORT_RANDOM_LENGTH; i++) {
            sb.append(REPORT_ALPHABET.charAt(random.nextInt(REPORT_ALPHABET.length())));
        }
        return sb.toString();
    }

    static String petCode(int year, long sequence) {
        return String.format("TH-%d-%04d", year, sequence);
    }
}
