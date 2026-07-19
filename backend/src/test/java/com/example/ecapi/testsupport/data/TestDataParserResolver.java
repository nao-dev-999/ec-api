package com.example.ecapi.testsupport.data;

/**
 * フォーマットに応じたパーサーを解決する。
 *
 * <p>新しいフォーマット（例: JSON）を追加する場合はここに分岐を1つ足すだけでよい。 CSV のみテーブル名の補完が必要なため {@link TestDataExtension}
 * 側で個別に生成する。
 */
public final class TestDataParserResolver {

    private TestDataParserResolver() {}

    public static TestDataParser resolve(TestDataFormat format) {
        return switch (format) {
            case YAML -> new YamlTestDataParser();
            case EXCEL -> new ExcelTestDataParser();
            case CSV ->
                    throw new IllegalStateException(
                            "CSV はテーブル名の補完が必要なため resolveForCsv() を使用してください");
            case AUTO -> throw new IllegalStateException("AUTO は事前に解決済みである必要があります");
        };
    }

    public static TestDataParser resolveForCsv(String tableName) {
        return new CsvTestDataParser(tableName);
    }
}
