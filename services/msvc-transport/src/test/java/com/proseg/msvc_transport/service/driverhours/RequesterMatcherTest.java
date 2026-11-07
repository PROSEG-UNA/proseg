package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static org.assertj.core.api.Assertions.assertThat;

class RequesterMatcherTest {

    private final Driver daniel = driver("Daniel Zúñiga Álvarez");
    private final Driver allan = driver("Allan Sánchez Gamboa");
    private final Driver ricardo = driver("Ricardo Jovane Solano");
    private final RequesterMatcher matcher = new RequesterMatcher(List.of(daniel, allan, ricardo));

    @Test
    void matchesIgnoringAccentsCaseAndSpacing() {
        assertThat(matcher.match("Daniel Zuniga")).contains(daniel);
        assertThat(matcher.match("  ALLAN   sanchez ")).contains(allan);
        assertThat(matcher.match("ricardo jovane solano")).contains(ricardo);
    }

    @Test
    void matchesWhenTheRequesterHasMoreWordsThanTheCatalogName() {
        assertThat(new RequesterMatcher(List.of(driver("Luis Loria"))).match("Luis A. Loria")).isPresent();
    }

    @Test
    void ignoresNamesThatShareOnlyOneWordOrHaveNoCatalogMatch() {
        assertThat(matcher.match("Daniel Mora")).isEmpty();
        assertThat(matcher.match("Ricardo Orozco")).isEmpty();
        assertThat(matcher.match("Ricardo")).isEmpty();
        assertThat(matcher.match("Sanchez")).isEmpty();
    }

    @Test
    void ignoresBlankOrMissingRequesters() {
        assertThat(matcher.match(null)).isEmpty();
        assertThat(matcher.match("   ")).isEmpty();
    }

    @Test
    void ignoresAmbiguousNamesAndReportsEveryCandidate() {
        Driver anaMora = driver("Ana Pérez Mora");
        Driver anaSolis = driver("Ana Pérez Solís");
        RequesterMatcher ambiguous = new RequesterMatcher(List.of(anaMora, anaSolis));

        assertThat(ambiguous.match("Ana Perez")).isEmpty();
        assertThat(ambiguous.matches("Ana Perez")).containsExactly(anaMora, anaSolis);
        assertThat(ambiguous.match("Ana Perez Solis")).contains(anaSolis);
    }
}
