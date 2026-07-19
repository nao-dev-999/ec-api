package com.example.ecapi.testsupport.data;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel(.xlsx)形式のテストデータをパースする。
 *
 * <p>シート名 = テーブル名、1行目 = ヘッダー（カラム名）という規約で読む。 既存の Excel ベーステスト資産をこの規約に合わせてリネームするだけで、 このパーサー経由で {@link
 * TestDataLoader} に流し込めるようになる（=互換性を保った移行）。
 *
 * <p>build.gradle.kts に以下の依存追加が必要:
 *
 * <pre>{@code
 * testImplementation("org.apache.poi:poi-ooxml:5.3.0")
 * }</pre>
 */
public class ExcelTestDataParser implements TestDataParser {

    @Override
    public TestDataSet parse(InputStream input) throws Exception {
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();

        try (Workbook workbook = new XSSFWorkbook(input)) {
            for (Sheet sheet : workbook) {
                String tableName = sheet.getSheetName();
                List<Map<String, Object>> rows = new ArrayList<>();

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    continue;
                }
                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(cell.getStringCellValue());
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    for (int c = 0; c < headers.size(); c++) {
                        Cell cell = row.getCell(c);
                        rowData.put(headers.get(c), readCellValue(cell));
                    }
                    rows.add(rowData);
                }
                tables.put(tableName, rows);
            }
        }

        return new TestDataSet(tables, List.of());
    }

    private Object readCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case BLANK -> null;
            case FORMULA -> readFormulaValue(cell);
            default -> cell.toString();
        };
    }

    private Object readFormulaValue(Cell cell) {
        // 数式セルはキャッシュされた値を読む（テストデータで数式はあまり想定しないため簡易対応）
        return switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> cell.getStringCellValue();
            default -> null;
        };
    }
}
