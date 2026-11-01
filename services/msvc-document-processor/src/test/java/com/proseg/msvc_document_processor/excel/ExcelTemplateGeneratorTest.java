package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelTemplateGeneratorTest {

    private final ExcelTemplateGenerator generator = new ExcelTemplateGenerator();

    @Test
    void generates_headers_styles_formats_and_freeze_pane() throws Exception {
        SchemaDto schema = SchemaDto.builder().columns(List.of(
                ColumnSchemaDto.builder().attribute("fallback").names(List.of("Nombre")).type("TEXT").build(),
                ColumnSchemaDto.builder().attribute("date").names(List.of("Fecha")).type("DATE").build(),
                ColumnSchemaDto.builder().attribute("amount").names(List.of("Monto")).type("DECIMAL").build(),
                ColumnSchemaDto.builder().attribute("text-2").names(List.of("Alias nombre")).type("TEXT").build(),
                ColumnSchemaDto.builder().attribute("fallback-header").names(List.of()).type(null).build(),
                ColumnSchemaDto.builder().attribute("fallback-name").names(null).type("OTHER").build(),
                ColumnSchemaDto.builder().attribute("fallback-null").names(java.util.Arrays.asList((String) null)).type("OTHER").build()
        )).build();

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(generator.generate(schema)))) {
            var sheet = workbook.getSheet("Plantilla");
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nombre");
            assertThat(sheet.getRow(0).getCell(4).getStringCellValue()).isEqualTo("fallback-header");
            assertThat(sheet.getRow(0).getCell(5).getStringCellValue()).isEqualTo("fallback-name");
            assertThat(sheet.getRow(0).getCell(6).getStringCellValue()).isEqualTo("fallback-null");
            assertThat(sheet.getColumnWidth(0)).isEqualTo(40 * 256);
            assertThat(sheet.getColumnStyle(3).getDataFormatString()).isEqualTo("@");
            assertThat(sheet.getColumnStyle(0).getDataFormatString()).isEqualTo("@");
            assertThat(sheet.getColumnStyle(1).getDataFormatString()).isEqualTo("dd/mm/yyyy");
            assertThat(sheet.getColumnStyle(2).getDataFormatString()).isEqualTo("General");
        }
    }

    @Test
    void generates_a_valid_empty_template_for_null_schema_or_columns() throws Exception {
        for (SchemaDto schema : List.of(
                SchemaDto.builder().columns(null).build(),
                SchemaDto.builder().columns(List.of()).build())) {
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(generator.generate(schema)))) {
                assertThat(workbook.getSheetAt(0).getRow(0).getLastCellNum()).isEqualTo((short) -1);
            }
        }
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(generator.generate(null)))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
        }
    }
}
