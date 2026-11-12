package com.proseg.msvc_document_processor.excel.reader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrokenAccentRepairerTest {

    private static final List<String> LOST_BIT_EVIDENCE = List.of(
            "acadC)micos", "asignaciC3n", "telC)fono", "dC-a"
    );

    @Test
    void decodesUtf8ReadAsWindows1252EvenWhenMixedWithCorrectAccents() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(List.of());

        assertThat(repairer.repair("acad\u00c3\u00a9micos y canci\u00f3n")).isEqualTo("académicos y canción");
        assertThat(repairer.repair("\u00c2\u00bfC\u00c3\u00b3mo est\u00c3\u00a1s? A\u00c3\u00b1o \u00e2\u20ac\u201c fin"))
                .isEqualTo("¿Cómo estás? Año – fin");
        assertThat(repairer.repair("\u00c3\u0192\u00c2\u00a9")).isEqualTo("é");
    }

    @Test
    void leavesTextWithoutValidUtf8SequencesUntouched() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(List.of());

        assertThat(repairer.repair("\u00c3")).isEqualTo("\u00c3");
        assertThat(repairer.repair("\u00c3 y más")).isEqualTo("\u00c3 y más");
        assertThat(repairer.repair("Ñandú, canción, Éxito")).isEqualTo("Ñandú, canción, Éxito");
        assertThat(repairer.repair(null)).isNull();
    }

    @Test
    void restoresLowercaseAccentsWhenTheHighBitWasLost() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(LOST_BIT_EVIDENCE);

        assertThat(repairer.repair("Viajan los acadC)micos: Alex ZC:C1iga, cC)dula 1154 MC3nica HernC!ndez QuirC3s"))
                .isEqualTo("Viajan los académicos: Alex Zúñiga, cédula 1154 Mónica Hernández Quirós");
        assertThat(repairer.repair("Es importante seC1alar el aC1o y la compaC1C-a, tambiC)n pingC<ino"))
                .isEqualTo("Es importante señalar el año y la compañía, también pingüino");
        assertThat(repairer.repair("el C:ltimo dC-a y la C!rea de CC3rdoba"))
                .isEqualTo("el último día y la área de Córdoba");
    }

    @Test
    void restoresUppercaseAccentsEncodedAsControlCharacters() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(LOST_BIT_EVIDENCE);

        assertThat(repairer.repair("MUC\u0011OZ FERNC\u0001NDEZ CONFIRMACIC\u0013N C\u0001REA"))
                .isEqualTo("MUÑOZ FERNÁNDEZ CONFIRMACIÓN ÁREA");
    }

    @Test
    void restoresCedillaAndUppercaseLettersThatWereEncodedTwice() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(LOST_BIT_EVIDENCE);

        assertThat(repairer.repair("un vehiC'ulo grande")).isEqualTo("un vehiçulo grande");
        assertThat(repairer.repair("MARIA CONCEPCIb\u0008\u001AC,N y ACUb\u0008\u001AC+A"))
                .isEqualTo("MARIA CONCEPCIÓN y ACUÑA");
    }

    @Test
    void restoresTypographicAndInvertedPunctuation() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(LOST_BIT_EVIDENCE);

        assertThat(repairer.repair("ArapiquC- b\u0013 CUNLimC3n y b\u0000\u0013 fin b\u0019s"))
                .isEqualTo("Arapiquí – CUNLimón y – fin ’s");
        assertThat(repairer.repair("B?CuC!ntos buses? B!Hola"))
                .isEqualTo("¿Cuántos buses? ¡Hola");
    }

    @Test
    void resolvesCapitalAcuteVowelsWhenTheLostCharacterBecameASpace() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(LOST_BIT_EVIDENCE);

        assertThat(repairer.repair("LC ASE: VEHC CULO, TELC FONO, DC AS, PC REZ, JIMC NEZ, PRC STAMO"))
                .isEqualTo("LÉASE: VEHÍCULO, TELÉFONO, DÍAS, PÉREZ, JIMÉNEZ, PRÉSTAMO");
    }

    @Test
    void usesAccentedWordsSeenElsewhereInTheFileToResolveCapitalVowels() {
        List<String> texts = new ArrayList<>(LOST_BIT_EVIDENCE);
        texts.add("Carlos Mejía");
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(texts);

        assertThat(repairer.repair("SE REQUIERE A MEJC A")).isEqualTo("SE REQUIERE A MEJÍA");
    }

    @Test
    void guessesAcuteEOnlyWhenTheWholeFileIsClearlyBroken() {
        List<String> fewEvidences = LOST_BIT_EVIDENCE;
        List<String> manyEvidences = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            manyEvidences.add("asignaciC3n " + i);
        }

        assertThat(BrokenAccentRepairer.analyze(fewEvidences).repair("ZORC NEZ")).isEqualTo("ZORC NEZ");
        assertThat(BrokenAccentRepairer.analyze(manyEvidences).repair("ZORC NEZ")).isEqualTo("ZORÉNEZ");
    }

    @Test
    void ignoresIsolatedLookalikesWhenThereIsNotEnoughEvidence() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(List.of("acadC)micos", "Opción C) Aula"));

        assertThat(repairer.repair("acadC)micos")).isEqualTo("acadC)micos");
    }

    @Test
    void keepsLegitimateTextUntouchedEvenInsideABrokenFile() {
        BrokenAccentRepairer repairer = BrokenAccentRepairer.analyze(LOST_BIT_EVIDENCE);
        List<String> legitimate = List.of(
                "Opción C) Aula", "Sector C3 y C1", "Bus ABC-123", "Placa BC1234", "VD-DISC-002-2", "NRC: 4314",
                "Disco C:\\datos", "X0C17W38", "Grupo C-12", "ABC DEF", "Vitamina C y D", "Tel 8585-6113", "Cédula 1-0234"
        );

        for (String text : legitimate) {
            assertThat(repairer.repair(text)).isEqualTo(text);
        }
    }
}
