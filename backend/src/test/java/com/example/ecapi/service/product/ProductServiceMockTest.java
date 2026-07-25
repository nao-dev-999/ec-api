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
 * データ駆動テストの利用例（パターンB: DBを使わずリポジトリをモック化してService単体をテストする場合）。
 *
 * <p>{@code @MockitoBean} はクラス全体のApplicationContextに適用されるため、実DBを使うテスト（{@link
 * ProductServiceIntegrationTest}）とはクラスを分離している。
 */
@DataDrivenTest
class ProductServiceMockTest {

    @Autowired private ProductService productService;

    @MockitoBean private ProductRepository productRepository;

    @Test
    @TestData(value = "testdata/product/get-product-mock.yml", format = TestDataFormat.YAML)
    void getProduct_whenMocked_returnsProduct() {
        var result = productService.findById(1L);

        assertThat(result.name()).isEqualTo("商品A");
    }
}
