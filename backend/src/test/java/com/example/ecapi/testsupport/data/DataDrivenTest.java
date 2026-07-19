package com.example.ecapi.testsupport.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * データ駆動テストを行うテストクラスに付与する合成アノテーション。
 *
 * <p>{@code @ExtendWith({SpringExtension.class, TestDataExtension.class})} を
 * 毎回書く手間と順序ミスを防ぐためのショートカット。
 *
 * <pre>{@code
 * @DataDrivenTest
 * class ProductServiceIntegrationTest {
 *
 *     @Autowired ProductService productService;
 *
 *     @Test
 *     @TestData("testdata/product/get-product.yml")
 *     void getProduct_returnsProduct() { ... }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ExtendWith({SpringExtension.class, TestDataExtension.class})
public @interface DataDrivenTest {}
