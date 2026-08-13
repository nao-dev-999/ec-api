package com.example.ecapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.config.JpaAuditConfig;
import com.example.ecapi.entity.Product;
import com.example.ecapi.repository.support.SoftDeleteJpaConfig;
import com.example.ecapi.support.TestcontainersConfiguration;
import java.math.BigDecimal;
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
