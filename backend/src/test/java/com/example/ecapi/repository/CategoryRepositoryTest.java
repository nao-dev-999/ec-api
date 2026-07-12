package com.example.ecapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.config.JpaAuditConfig;
import com.example.ecapi.entity.Category;
import com.example.ecapi.support.TestcontainersConfiguration;
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
@Import({TestcontainersConfiguration.class, JpaAuditConfig.class})
class CategoryRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private CategoryRepository categoryRepository;

    private Category persistCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return entityManager.persistFlushFind(category);
    }

    @Nested
    @DisplayName("existsByName")
    class ExistsByNameTest {

        @Test
        @DisplayName("同名のカテゴリが存在する場合、true を返すこと")
        void shouldReturnTrueWhenNameExists() {
            persistCategory("家電");

            assertThat(categoryRepository.existsByName("家電")).isTrue();
        }

        @Test
        @DisplayName("同名のカテゴリが存在しない場合、false を返すこと")
        void shouldReturnFalseWhenNameDoesNotExist() {
            assertThat(categoryRepository.existsByName("存在しないカテゴリ")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByNameAndIdNot")
    class ExistsByNameAndIdNotTest {

        @Test
        @DisplayName("自分自身の ID を除外した場合、false を返すこと")
        void shouldReturnFalseWhenExcludingOwnId() {
            Category category = persistCategory("食品");

            assertThat(categoryRepository.existsByNameAndIdNot("食品", category.getId())).isFalse();
        }

        @Test
        @DisplayName("自分以外の ID で同名が存在する場合、true を返すこと")
        void shouldReturnTrueWhenAnotherIdHasSameName() {
            Category category = persistCategory("食品");

            assertThat(categoryRepository.existsByNameAndIdNot("食品", category.getId() + 1))
                    .isTrue();
        }
    }
}
