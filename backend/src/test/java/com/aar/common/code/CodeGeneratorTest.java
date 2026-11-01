package com.aar.common.code;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class CodeGeneratorTest {

    private final SecureRandom random = new SecureRandom();

    @RepeatedTest(50)
    void reportCodeHasPrefixAndNoAmbiguousCharacters() {
        String code = CodeGenerator.reportCode(random);
        assertThat(code).matches("BC-[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}");
        assertThat(code.length()).isLessThanOrEqualTo(12);
    }

    @Test
    void petCodeIsZeroPaddedAndFitsColumn() {
        assertThat(CodeGenerator.petCode(2026, 7)).isEqualTo("TH-2026-0007");
        assertThat(CodeGenerator.petCode(2026, 1234)).hasSize(12);
    }
}
