package com.example.ecapi.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;

/**
 * BusinessException 階層の規約（docs/06-exception-handling.md 6.1）を機械的に検査する。
 *
 * <p>新しい例外クラスを追加すると自動でこのテストの対象になる。
 */
class BusinessExceptionHierarchyTest {

    private static final Map<Class<?>, HttpStatus> INTERMEDIATE_STATUS =
            Map.of(
                    BadRequestException.class, HttpStatus.BAD_REQUEST,
                    ResourceNotFoundException.class, HttpStatus.NOT_FOUND,
                    ConflictException.class, HttpStatus.CONFLICT);

    static Stream<Class<?>> concreteBusinessExceptions() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BusinessException.class));
        List<Class<?>> classes =
                scanner.findCandidateComponents(BusinessException.class.getPackageName()).stream()
                        .<Class<?>>map(
                                bd -> ClassUtils.resolveClassName(bd.getBeanClassName(), null))
                        .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                        .toList();
        assertThat(classes).as("具象ビジネス例外が1つも見つからない").isNotEmpty();
        return classes.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("concreteBusinessExceptions")
    @DisplayName("具象例外は中間抽象クラスを継承し、そのステータスを返すこと")
    void concreteException_shouldExtendIntermediateClassAndMatchItsStatus(Class<?> type)
            throws Exception {
        Class<?> parent = type.getSuperclass();
        assertThat(INTERMEDIATE_STATUS)
                .as("%s は BusinessException を直接継承せず、中間抽象クラスを継承すること", type.getSimpleName())
                .containsKey(parent);

        BusinessException ex = instantiate(type);

        assertThat(ex.getStatus()).isEqualTo(INTERMEDIATE_STATUS.get(parent));
    }

    @Test
    @DisplayName("中間抽象クラスと異なるステータスの ErrorCode を渡すと例外になること")
    void requireStatus_whenStatusMismatch_shouldThrow() {
        // 404 の中間クラスに 409 の ErrorCode を渡す誤り。
        // ローカルクラスにしてクラスパススキャンの対象から外す
        class MisconfiguredNotFoundException extends ResourceNotFoundException {
            MisconfiguredNotFoundException() {
                super(ErrorCode.PRODUCT_IN_USE);
            }
        }

        assertThatThrownBy(MisconfiguredNotFoundException::new)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRODUCT_IN_USE");
    }

    @Test
    @DisplayName("中間抽象クラスと同じステータスの ErrorCode はそのまま使えること")
    void requireStatus_whenStatusMatches_shouldKeepErrorCode() {
        var ex = new ProductNotFoundException(1L);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        assertThat(ex.getArgs()).containsExactly(1L);
    }

    private static BusinessException instantiate(Class<?> type) throws Exception {
        Constructor<?> ctor =
                Arrays.stream(type.getConstructors())
                        .filter(c -> c.getParameterCount() == 0 || c.isVarArgs())
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                type.getSimpleName()
                                                        + " に引数なし or 可変長引数のコンストラクタがない"));
        Object[] args =
                ctor.getParameterCount() == 0 ? new Object[0] : new Object[] {new Object[0]};
        return (BusinessException) ctor.newInstance(args);
    }
}
