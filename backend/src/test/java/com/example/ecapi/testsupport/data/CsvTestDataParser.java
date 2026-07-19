package com.example.ecapi.testsupport.data;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV形式のテストデータをパースする。1ファイル = 1テーブルというシンプルな制約を置く。
 *
 * <p>テーブル名は呼び出し元（{@link TestDataExtension}）がファイル名から補完する前提のため、 ここでは行データのみを組み立て、呼び出し側で "tables"
 * のキーに割り当てる。
 *
 * <p>注意: このサンプル実装はダブルクォート内カンマ等のエスケープに対応していない簡易パーサー。 実運用では Apache Commons CSV / OpenCSV への差し替えを推奨する。
 */
public class CsvTestDataParser implements TestDataParser {

    /** CSVファイル名（拡張子なし）をテーブル名として扱うためのフック。Extension側でセットする。 */
    private final String tableName;

    public CsvTestDataParser(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public TestDataSet parse(InputStream input) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new TestDataSet(Map.of(tableName, rows), List.of());
            }
            String[] headers = headerLine.split(",", -1);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                rows.add(row);
            }
        }

        return new TestDataSet(Map.of(tableName, rows), List.of());
    }
}
