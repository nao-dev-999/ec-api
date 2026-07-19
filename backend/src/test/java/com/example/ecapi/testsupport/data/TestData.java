package com.example.ecapi.testsupport.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * テストメソッドにデータ駆動テストのデータソースを紐付けるアノテーション。
 *
 * <p>拡張子（.yml / .yaml / .csv / .xlsx）からフォーマットを自動判定するため、 既存の Excel ベーステストを壊さずに YAML 版を並行して追加できる。
 *
 * <pre>{@code
 * @DataDrivenTest
 * class ProductServiceIntegrationTest {
 *
 *     @Test
 *     @TestData("testdata/product/get-product.yml")
 *     void getProduct_returnsProduct() { ... }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestData {

    /** クラスパス上のテストデータファイルパス（例: "testdata/product/get-product.yml"） */
    String value();

    /** フォーマットを明示したい場合に指定する。デフォルトは拡張子からの自動判定 */
    TestDataFormat format() default TestDataFormat.AUTO;

    /** テスト終了後にロードしたテーブルを自動でクリーンアップ（DELETE）するか */
    boolean cleanupAfter() default true;
}
