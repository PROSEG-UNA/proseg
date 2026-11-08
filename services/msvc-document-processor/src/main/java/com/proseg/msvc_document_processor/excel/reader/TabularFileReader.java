package com.proseg.msvc_document_processor.excel.reader;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class TabularFileReader {

    private static final List<String> ACCEPTED_EXTENSIONS = List.of(".xlsx", ".xls", ".xlsm", ".csv");
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] OLE2_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final int HTML_SNIFF_LENGTH = 4096;
    private static final int MAX_COLSPAN = 100;
    private static final char[] CSV_DELIMITER_CANDIDATES = {';', ',', '\t'};

    public TabularFile read(String fileName, byte[] content) {
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (ACCEPTED_EXTENSIONS.stream().noneMatch(lowerName::endsWith)) {
            throw DocumentProcessorException.invalidTourFileType();
        }
        SpreadsheetFormat format = detectFormat(lowerName, content);
        List<SheetRow> rows = switch (format) {
            case XLSX, XLSM, XLS -> readWorkbook(content);
            case HTML -> readHtmlTable(content);
            case CSV -> readCsv(content);
        };
        return new TabularFile(format, rows);
    }

    SpreadsheetFormat detectFormat(String lowerName, byte[] content) {
        if (LenientTextDecoder.startsWith(content, ZIP_SIGNATURE)) {
            return lowerName.endsWith(".xlsm") ? SpreadsheetFormat.XLSM : SpreadsheetFormat.XLSX;
        }
        if (LenientTextDecoder.startsWith(content, OLE2_SIGNATURE)) {
            return SpreadsheetFormat.XLS;
        }
        if (looksLikeHtml(content)) {
            return SpreadsheetFormat.HTML;
        }
        if (lowerName.endsWith(".csv")) {
            return SpreadsheetFormat.CSV;
        }
        throw DocumentProcessorException.unsupportedFileFormat();
    }

    private boolean looksLikeHtml(byte[] content) {
        String head = new String(content, 0, Math.min(content.length, HTML_SNIFF_LENGTH), StandardCharsets.ISO_8859_1)
                .replace("ï»¿", "")
                .stripLeading()
                .toLowerCase(Locale.ROOT);
        return head.startsWith("<") && (head.contains("<table") || head.contains("<html"));
    }

    private List<SheetRow> readWorkbook(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            formatter.setUseCachedValuesForFormulaCells(true);
            List<SheetRow> rows = new ArrayList<>();
            for (Row row : sheet) {
                int lastCell = Math.max(row.getLastCellNum(), 0);
                List<SheetCell> cells = new ArrayList<>(lastCell);
                for (int c = 0; c < lastCell; c++) {
                    cells.add(toSheetCell(row.getCell(c), formatter));
                }
                rows.add(new SheetRow(row.getRowNum() + 1, cells));
            }
            return rows;
        } catch (IOException | RuntimeException e) {
            throw DocumentProcessorException.unreadableWorkbook();
        }
    }

    private SheetCell toSheetCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return SheetCell.empty();
        }
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
        String text = formatter.formatCellValue(cell);
        if (type != CellType.NUMERIC) {
            return SheetCell.ofText(text);
        }
        LocalDateTime dateValue = DateUtil.isCellDateFormatted(cell) ? cell.getLocalDateTimeCellValue() : null;
        return SheetCell.ofNumber(text, cell.getNumericCellValue(), dateValue);
    }

    private List<SheetRow> readHtmlTable(byte[] content) {
        Element table = Jsoup.parse(LenientTextDecoder.decode(content)).selectFirst("table");
        if (table == null) {
            throw DocumentProcessorException.htmlTableNotFound();
        }
        List<SheetRow> rows = new ArrayList<>();
        int rowNumber = 0;
        for (Element tr : table.select("tr")) {
            rowNumber++;
            List<SheetCell> cells = new ArrayList<>();
            for (Element cell : tr.children()) {
                if (!cell.nameIs("td") && !cell.nameIs("th")) {
                    continue;
                }
                cells.add(SheetCell.ofText(cell.text()));
                for (int i = 1; i < colspan(cell); i++) {
                    cells.add(SheetCell.empty());
                }
            }
            rows.add(new SheetRow(rowNumber, cells));
        }
        return rows;
    }

    private int colspan(Element cell) {
        String value = cell.attr("colspan").trim();
        if (!value.matches("\\d{1,3}")) {
            return 1;
        }
        return Math.max(1, Math.min(Integer.parseInt(value), MAX_COLSPAN));
    }

    private List<SheetRow> readCsv(byte[] content) {
        String text = LenientTextDecoder.decode(content);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectCsvDelimiter(text))
                .setIgnoreEmptyLines(false)
                .build();
        List<SheetRow> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(text, format)) {
            for (CSVRecord record : parser) {
                List<SheetCell> cells = new ArrayList<>(record.size());
                record.forEach(value -> cells.add(SheetCell.ofText(value)));
                rows.add(new SheetRow((int) record.getRecordNumber(), cells));
            }
        } catch (IOException | RuntimeException e) {
            throw DocumentProcessorException.unreadableWorkbook();
        }
        return rows;
    }

    private char detectCsvDelimiter(String text) {
        String firstLine = text.lines()
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("");
        char best = ',';
        int bestCount = 0;
        for (char candidate : CSV_DELIMITER_CANDIDATES) {
            int count = countOutsideQuotes(firstLine, candidate);
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        return best;
    }

    private int countOutsideQuotes(String line, char delimiter) {
        int count = 0;
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                quoted = !quoted;
            } else if (current == delimiter && !quoted) {
                count++;
            }
        }
        return count;
    }
}
