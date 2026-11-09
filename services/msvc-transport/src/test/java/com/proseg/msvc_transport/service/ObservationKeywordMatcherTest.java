package com.proseg.msvc_transport.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationKeywordMatcherTest {

    @Test
    void matchesIgnoringCase() {
        assertThat(ObservationKeywordMatcher.matches("Revisar URGENTE hoy", "urgente")).isTrue();
    }

    @Test
    void matchesIgnoringAccents() {
        assertThat(ObservationKeywordMatcher.matches("Revisar urgénte hoy", "urgente")).isTrue();
        assertThat(ObservationKeywordMatcher.matches("Revisar urgente hoy", "urgénte")).isTrue();
    }

    @Test
    void matchesAPartialText() {
        assertThat(ObservationKeywordMatcher.matches("Es URGENTE revisar", "urge")).isTrue();
        assertThat(ObservationKeywordMatcher.matches("muy urgente", "urgente")).isTrue();
    }

    @Test
    void collapsesSpacesAndRejectsADifferentWord() {
        assertThat(ObservationKeywordMatcher.normalize("  UR  GENTE  ")).isEqualTo("ur gente");
        assertThat(ObservationKeywordMatcher.matches("  Es   URGENTE ", "urgente")).isTrue();
        assertThat(ObservationKeywordMatcher.matches("sin novedad", "urgente")).isFalse();
        assertThat(ObservationKeywordMatcher.matches(null, "urgente")).isFalse();
        assertThat(ObservationKeywordMatcher.matches("urgente", "   ")).isFalse();
    }

    @Test
    void returnsOnlyTheKeywordsThatMatch() {
        assertThat(ObservationKeywordMatcher.matching("URGENTE y pendiente", List.of("urgente", "mañana", "pend")))
                .containsExactly("urgente", "pend");
    }
}
