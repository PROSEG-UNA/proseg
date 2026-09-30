package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.excel.exception.InvalidCellValueException;
import com.proseg.msvc_document_processor.excel.reader.SheetCell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CellValueConverterTest {

    @ParameterizedTest
    @ValueSource(strings = {"01-05-2026", "1-5-2026", "01/05/2026", "1/5/2026", "2026-05-01"})
    void parsesSupportedDateFormats(String text) {
        assertThat(CellValueConverter.convert(SheetCell.ofText(text), "DATE", "Fecha Salida"))
                .isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void rejectsNonExistentDate() {
        assertThatThrownBy(() -> CellValueConverter.convert(SheetCell.ofText("31-04-2026"), "DATE", "Fecha Salida"))
                .isInstanceOf(InvalidCellValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"06:00", "6:00", "06:00:00", "6:00 AM", "6:00 a. m.", "6:00 am"})
    void parsesSupportedMorningTimes(String text) {
        assertThat(CellValueConverter.convert(SheetCell.ofText(text), "TIME", "Hora Salida"))
                .isEqualTo(LocalTime.of(6, 0));
    }

    @Test
    void parsesAfternoonTimeWithMeridiem() {
        assertThat(CellValueConverter.convert(SheetCell.ofText("4:30 p. m."), "TIME", "Hora Regreso"))
                .isEqualTo(LocalTime.of(16, 30));
    }

    @Test
    void usesNativeDateAndTimeCellValues() {
        SheetCell cell = SheetCell.ofNumber("irrelevante", 46143.25, LocalDateTime.of(2026, 5, 1, 6, 0));

        assertThat(CellValueConverter.convert(cell, "DATE", "Fecha Salida")).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(CellValueConverter.convert(cell, "TIME", "Hora Salida")).isEqualTo(LocalTime.of(6, 0));
    }

    @Test
    void parsesIntegersFromNumbersAndText() {
        assertThat(CellValueConverter.convert(SheetCell.ofNumber("1,234", 1234d, null), "INTEGER", "Pasajeros"))
                .isEqualTo(1234);
        assertThat(CellValueConverter.convert(SheetCell.ofText("45.0"), "INTEGER", "Pasajeros")).isEqualTo(45);
    }

    @Test
    void rejectsNonIntegerValues() {
        assertThatThrownBy(() -> CellValueConverter.convert(SheetCell.ofText("4.5"), "INTEGER", "Pasajeros"))
                .isInstanceOf(InvalidCellValueException.class)
                .hasMessage("Valor entero inválido en la columna 'Pasajeros': '4.5'");
    }

    @Test
    void keepsTextAsIs() {
        assertThat(CellValueConverter.convert(SheetCell.ofText("  04504 "), "TEXT", "Numero")).isEqualTo("04504");
    }
}
