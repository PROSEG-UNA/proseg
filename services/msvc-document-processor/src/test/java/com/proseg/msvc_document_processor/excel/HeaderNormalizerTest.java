package com.proseg.msvc_document_processor.excel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderNormalizerTest {

    @Test
    void normalizes_case_accents_whitespace_and_null() {
        assertThat(HeaderNormalizer.normalize("  NÚMERO   DE   ACTIVO ")).isEqualTo("numero de activo");
        assertThat(HeaderNormalizer.normalize(null)).isEmpty();
        assertThat(HeaderNormalizer.normalize("")).isEmpty();
    }
}
