package com.example.ecapi.testsupport.data;

/** テストデータファイルのフォーマット。AUTO の場合は拡張子から判定する。 */
public enum TestDataFormat {
    YAML,
    CSV,
    EXCEL,
    AUTO;

    public static TestDataFormat fromLocation(String location) {
        String lower = location.toLowerCase();
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return YAML;
        }
        if (lower.endsWith(".csv")) {
            return CSV;
        }
        if (lower.endsWith(".xlsx")) {
            return EXCEL;
        }
        throw new IllegalArgumentException("拡張子からフォーマットを判定できません。format() を明示してください: " + location);
    }
}
