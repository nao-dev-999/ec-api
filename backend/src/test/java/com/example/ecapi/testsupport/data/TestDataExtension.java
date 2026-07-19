package com.example.ecapi.testsupport.data;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * {@link TestData} アノテーションを見つけて、テスト実行前にデータを読み込み、 実行後にクリーンアップする JUnit5 拡張。
 *
 * <p><b>SpringExtension を継承せず、必ず並列合成で使う。</b>
 *
 * <pre>{@code
 * @ExtendWith({SpringExtension.class, TestDataExtension.class})
 * @SpringBootTest
 * class SomeTest { ... }
 * }</pre>
 *
 * SpringExtension が先に ApplicationContext を確立している必要があるため、 {@code @ExtendWith} の配列内では SpringExtension
 * を先に書くこと（実運用では {@link DataDrivenTest} のような合成アノテーションでこの順序ミスを防ぐ）。
 */
public class TestDataExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestDataExtension.class);
    private static final String STORE_KEY = "loadedTables";

    @Override
    public void beforeEach(ExtensionContext context) {
        AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), TestData.class)
                .ifPresent(annotation -> applyTestData(context, annotation));
    }

    private void applyTestData(ExtensionContext context, TestData annotation) {
        ApplicationContext springContext = SpringExtension.getApplicationContext(context);
        TestDataLoader loader = springContext.getBean(TestDataLoader.class);

        String location = annotation.value();
        TestDataFormat format =
                annotation.format() == TestDataFormat.AUTO
                        ? TestDataFormat.fromLocation(location)
                        : annotation.format();

        TestDataParser parser =
                format == TestDataFormat.CSV
                        ? TestDataParserResolver.resolveForCsv(extractTableName(location))
                        : TestDataParserResolver.resolve(format);

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(location)) {
            if (input == null) {
                throw new IllegalStateException("テストデータがクラスパス上に見つかりません: " + location);
            }
            TestDataSet dataSet = parser.parse(input);

            loader.loadTables(dataSet);
            loader.loadMocks(dataSet, springContext);

            if (annotation.cleanupAfter()) {
                getLoadedTablesStack(context).push(dataSet.tables().keySet());
            }
        } catch (Exception e) {
            throw new IllegalStateException("テストデータの読み込みに失敗しました: " + location, e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ApplicationContext springContext = SpringExtension.getApplicationContext(context);
        TestDataLoader loader = springContext.getBean(TestDataLoader.class);

        Deque<Set<String>> stack = getLoadedTablesStack(context);
        while (!stack.isEmpty()) {
            loader.cleanup(stack.pop());
        }
    }

    private String extractTableName(String location) {
        String fileName = location.substring(location.lastIndexOf('/') + 1);
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    @SuppressWarnings("unchecked")
    private Deque<Set<String>> getLoadedTablesStack(ExtensionContext context) {
        return (Deque<Set<String>>)
                context.getStore(NAMESPACE)
                        .getOrComputeIfAbsent(STORE_KEY, key -> new ArrayDeque<>());
    }
}
