package com.example.ecapi.service.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.testsupport.data.DataDrivenTest;
import com.example.ecapi.testsupport.data.TestData;
import com.example.ecapi.testsupport.data.TestDataFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * データ駆動テストの利用例。
 *
 * <p>{@link DataDrivenTest} を付けるだけで SpringExtension + TestDataExtension が有効になる。 各テストメソッドは {@link
 * TestData} で自分が使うデータファイルだけを宣言すればよい。
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
        assertThat(result.price()).isEqualTo(1000);
    }

    // --- パターンB: DBを使わずリポジトリをモック化してService単体をテストする場合 ---
    @MockitoBean private ProductRepository productRepository;

    @Test
    @TestData(value = "testdata/product/get-product-mock.yml", format = TestDataFormat.YAML)
    void getProduct_whenMocked_returnsProduct() {
        var result = productService.findById(1L);

        assertThat(result.name()).isEqualTo("商品A");
    }

    // --- パターンC: Excel資産をそのまま流用する場合（既存互換） ---
    @Test
    @TestData("testdata/product/get-product.xlsx")
    void getProduct_fromExcelFixture_returnsProduct() {
        var result = productService.findById(1L);

        assertThat(result.name()).isEqualTo("商品A");
    }
}
