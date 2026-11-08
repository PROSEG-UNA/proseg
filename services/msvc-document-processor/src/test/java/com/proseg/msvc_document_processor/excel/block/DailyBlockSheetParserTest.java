package com.proseg.msvc_document_processor.excel.block;

import com.proseg.msvc_document_processor.dto.response.RowIssueDto;
import com.proseg.msvc_document_processor.excel.ParseResult;
import com.proseg.msvc_document_processor.excel.TourSchemaFixture;
import com.proseg.msvc_document_processor.excel.reader.SheetCell;
import com.proseg.msvc_document_processor.excel.reader.SheetRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DailyBlockSheetParserTest {

    private final DailyBlockSheetParser parser = new DailyBlockSheetParser();

    @Test
    void parsesRepeatedBlocksAndAssignsRepeatedHeadersInSchemaOrder() {
        List<SheetRow> rows = List.of(
                separator(1, "Viernes 1 de Mayo de 2026"),
                row(2, TourSchemaFixture.REAL_FILE_HEADERS),
                row(3, dataRow("Marvin Quesada", "04504", "301-617", "06:00", "12:00", "01-05-2026", "01-05-2026")),
                separator(4, "Sábado 2 de Mayo de 2026"),
                row(5, TourSchemaFixture.REAL_FILE_HEADERS),
                row(6, dataRow("", "01900", "Sedan", "08:00", "17:00", "02-05-2026", "03-05-2026"))
        );

        ParseResult result = parser.parse(rows, TourSchemaFixture.schema());

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getBlocksDetected()).isEqualTo(2);
        assertThat(result.getTotalRows()).isEqualTo(2);
        Map<String, Object> first = result.getRows().get(0);
        assertThat(first)
                .containsEntry("rowNumber", 3)
                .containsEntry("number", "04504")
                .containsEntry("driver", "Marvin Quesada")
                .containsEntry("passengers", 45)
                .containsEntry("departureTime", LocalTime.of(6, 0))
                .containsEntry("returnTime", LocalTime.of(12, 0))
                .containsEntry("departureDate", LocalDate.of(2026, 5, 1))
                .containsEntry("returnDate", LocalDate.of(2026, 5, 1))
                .doesNotContainKeys("perDay", "perMonth");
        assertThat(result.getRows().get(1))
                .doesNotContainKey("driver")
                .containsEntry("returnDate", LocalDate.of(2026, 5, 3));
    }

    @Test
    void acceptsExplicitHeadersInAnyOrder() {
        List<String> headers = new ArrayList<>(TourSchemaFixture.REAL_FILE_HEADERS);
        headers.set(12, "Fecha Salida");
        headers.set(13, "Fecha Regreso");
        headers.set(14, "Hora Salida");
        headers.set(15, "Hora Regreso");
        List<String> values = dataRow("Chofer", "1", "301-617", "01-05-2026", "02-05-2026", "06:00", "12:00");

        ParseResult result = parser.parse(List.of(row(1, headers), row(2, values)), TourSchemaFixture.schema());

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getRows().get(0))
                .containsEntry("departureDate", LocalDate.of(2026, 5, 1))
                .containsEntry("departureTime", LocalTime.of(6, 0));
    }

    @Test
    void reportsMissingRequiredColumnOnceAcrossBlocks() {
        List<String> headers = new ArrayList<>(TourSchemaFixture.REAL_FILE_HEADERS);
        headers.set(10, "");
        List<SheetRow> rows = List.of(
                separator(1, "Viernes 1 de Mayo de 2026"),
                row(2, headers),
                row(3, dataRow("A", "1", "301-617", "06:00", "12:00", "01-05-2026", "01-05-2026")),
                separator(4, "Sabado 2 de Mayo de 2026"),
                row(5, headers),
                row(6, dataRow("B", "2", "301-617", "06:00", "12:00", "02-05-2026", "02-05-2026"))
        );

        ParseResult result = parser.parse(rows, TourSchemaFixture.schema());

        assertThat(result.getErrors())
                .extracting(RowIssueDto::getRow, RowIssueDto::getReason)
                .containsExactly(tuple(2, "Falta la columna obligatoria 'Prioridad'"));
    }

    @Test
    void reportsUnknownColumn() {
        List<String> headers = new ArrayList<>(TourSchemaFixture.REAL_FILE_HEADERS);
        headers.add("Kilometraje");

        ParseResult result = parser.parse(List.of(row(1, headers)), TourSchemaFixture.schema());

        assertThat(result.getErrors())
                .extracting(RowIssueDto::getReason)
                .containsExactly("Columna no reconocida: 'Kilometraje'");
    }

    @Test
    void reportsInvalidValuesAndEmptyRequiredCellsPerRow() {
        List<String> invalidDate = dataRow("A", "1", "301-617", "06:00", "12:00", "31-04-2026", "01-05-2026");
        List<String> invalidTime = dataRow("B", "2", "301-617", "25:00", "12:00", "01-05-2026", "01-05-2026");
        List<String> invalidPassengers = dataRow("C", "3", "301-617", "06:00", "12:00", "01-05-2026", "01-05-2026");
        invalidPassengers.set(5, "abc");
        List<String> emptyNumber = dataRow("D", "", "301-617", "06:00", "12:00", "01-05-2026", "01-05-2026");

        ParseResult result = parser.parse(List.of(
                row(1, TourSchemaFixture.REAL_FILE_HEADERS),
                row(2, invalidDate),
                row(3, invalidTime),
                row(4, invalidPassengers),
                row(5, emptyNumber)
        ), TourSchemaFixture.schema());

        assertThat(result.getRows()).isEmpty();
        assertThat(result.getErrors())
                .extracting(RowIssueDto::getRow, RowIssueDto::getReason)
                .containsExactly(
                        tuple(2, "Fecha inválida en la columna 'Fecha Salida': '31-04-2026'"),
                        tuple(3, "Hora inválida en la columna 'Hora Salida': '25:00'"),
                        tuple(4, "Valor entero inválido en la columna 'Pasajeros': 'abc'"),
                        tuple(5, "El campo obligatorio 'Numero' está vacío")
                );
    }

    @Test
    void reportsDataBeforeHeaderOnce() {
        List<SheetRow> rows = List.of(
                separator(1, "Viernes 1 de Mayo de 2026"),
                row(2, dataRow("A", "1", "301-617", "06:00", "12:00", "01-05-2026", "01-05-2026")),
                row(3, dataRow("B", "2", "301-617", "06:00", "12:00", "01-05-2026", "01-05-2026"))
        );

        ParseResult result = parser.parse(rows, TourSchemaFixture.schema());

        assertThat(result.getErrors())
                .extracting(RowIssueDto::getRow, RowIssueDto::getReason)
                .containsExactly(tuple(2, "Fila de datos antes de la fila de encabezados"));
    }

    @Test
    void doesNotTreatDataRowWithDateTextAsSeparator() {
        List<String> values = dataRow("Viernes 5 de mayo de 2026", "1", "301-617", "06:00", "12:00", "05-05-2026", "05-05-2026");

        ParseResult result = parser.parse(List.of(
                row(1, TourSchemaFixture.REAL_FILE_HEADERS),
                row(2, values)
        ), TourSchemaFixture.schema());

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getBlocksDetected()).isZero();
        assertThat(result.getRows()).hasSize(1);
    }

    private List<String> dataRow(String driver, String number, String vehicle, String timeOrDate1, String timeOrDate2,
                                 String dateOrTime1, String dateOrTime2) {
        return new ArrayList<>(Arrays.asList(
                "", "", driver, number, vehicle, "45", "Vicerrectoria De Administracion", "Dennis Viquez",
                "Merced", "1", "10", "", timeOrDate1, timeOrDate2, dateOrTime1, dateOrTime2, "Observación"
        ));
    }

    private SheetRow separator(int rowNumber, String title) {
        List<String> values = new ArrayList<>(Arrays.asList(new String[17]));
        values.set(0, title);
        values.set(12, "Hora");
        values.set(14, "Fecha");
        return row(rowNumber, values);
    }

    private SheetRow row(int rowNumber, List<String> values) {
        return new SheetRow(rowNumber, values.stream().map(SheetCell::ofText).toList());
    }
}
