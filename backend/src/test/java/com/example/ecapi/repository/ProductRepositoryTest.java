package com.example.ecapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.config.JpaAuditConfig;
import com.example.ecapi.entity.Product;
import com.example.ecapi.repository.support.SoftDeleteJpaConfig;
import com.example.ecapi.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditConfig.class, SoftDeleteJpaConfig.class})
class ProductRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private ProductRepository productRepository;

    private Product persistProduct(String name, BigDecimal price) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("description");
        product.setPrice(price);
        product.setStock(10);
        return entityManager.persistFlushFind(product);
    }

    @Nested
    @DisplayName("findByNameContainingIgnoreCase")
    class FindByNameContainingIgnoreCaseTest {

        @Test
        @DisplayName("大文字小文字を無視して部分一致検索できること")
        void shouldReturnProductsMatchingKeywordIgnoringCase() {
            persistProduct("Apple Watch", BigDecimal.valueOf(50000));
            persistProduct("Wireless Mouse", BigDecimal.valueOf(3000));

            List<Product> result =
                    productRepository.findByNameContainingIgnoreCaseAndDeletedFalse("watch");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Apple Watch");
        }

        @Test
        @DisplayName("該当する商品がない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoMatch() {
            persistProduct("Apple Watch", BigDecimal.valueOf(50000));

            List<Product> result =
                    productRepository.findByNameContainingIgnoreCaseAndDeletedFalse("存在しない");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPriceRange")
    class FindByPriceRangeTest {

        @Test
        @DisplayName("価格帯に含まれる商品を価格の昇順で取得できること")
        void shouldReturnProductsWithinPriceRangeOrderedByPrice() {
            persistProduct("安い商品", BigDecimal.valueOf(100));
            Product middle = persistProduct("中間の商品", BigDecimal.valueOf(500));
            Product high = persistProduct("やや高い商品", BigDecimal.valueOf(800));
            persistProduct("高い商品", BigDecimal.valueOf(2000));

            List<Product> result =
                    productRepository.findByPriceRange(
                            BigDecimal.valueOf(200), BigDecimal.valueOf(1000));

            assertThat(result)
                    .extracting(Product::getId)
                    .containsExactly(middle.getId(), high.getId());
        }
    }

    @Nested
    @DisplayName("deleteById（論理削除）")
    class DeleteByIdTest {

        @Test
        @DisplayName("物理削除ではなくdeleted=trueへの更新になり、findByIdからは見えなくなること")
        void shouldSoftDeleteInsteadOfPhysicalDelete() {
            Product product = persistProduct("削除対象商品", BigDecimal.valueOf(1000));

            productRepository.deleteById(product.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(productRepository.findById(product.getId())).isEmpty();
            Optional<Product> raw =
                    Optional.ofNullable(entityManager.find(Product.class, product.getId()));
            assertThat(raw).isPresent();
            assertThat(raw.get().isDeleted()).isTrue();
        }
    }
}
