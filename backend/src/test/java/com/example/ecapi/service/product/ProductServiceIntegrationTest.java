package com.example.ecapi.service.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.testsupport.data.DataDrivenTest;
import com.example.ecapi.testsupport.data.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * データ駆動テストの利用例。
 *
 * <p>{@link DataDrivenTest} を付けるだけで SpringExtension + TestDataExtension が有効になる。 各テストメソッドは {@link
 * TestData} で自分が使うデータファイルだけを宣言すればよい。
 *
 * <p>{@code @MockitoBean} を持つクラス（{@link ProductServiceMockTest}）とはテストクラスを分ける。同一クラスに混在させると
 * {@code @MockitoBean} のフィールドがクラス全体のApplicationContextに適用され、実DBを使うテストでもリポジトリがモック化されてしまうため。
 */
@DataDrivenTest
class ProductServiceIntegrationTest {

    @Autowired private ProductService productService;

    // --- パターンA: 実DBにデータを投入して統合テストする場合 ---
    @Test
    @TestData("testdata/product/get-product.yml")
    void getProduct_returnsProduct() {
        var result = productService.findById(1L);

        assertThat(result.name()).isEqualTo("商品A");
        assertThat(result.price()).isEqualByComparingTo("1000");
    }

    // --- パターンC: Excel資産をそのまま流用する場合（既存互換） ---
    @Test
    @TestData("testdata/product/get-product.xlsx")
    void getProduct_fromExcelFixture_returnsProduct() {
        var result = productService.findById(1L);

        assertThat(result.name()).isEqualTo("商品A");
    }
}
