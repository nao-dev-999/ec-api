package com.example.ecapi.testsupport.data;

import java.io.InputStream;

/** テストデータファイルを共通モデル {@link TestDataSet} に変換するパーサー。 */
public interface TestDataParser {

    TestDataSet parse(InputStream input) throws Exception;
}
