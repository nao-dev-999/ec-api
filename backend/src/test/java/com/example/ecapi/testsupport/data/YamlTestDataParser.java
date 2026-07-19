package com.example.ecapi.testsupport.data;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * YAML形式のテストデータをパースする。
 *
 * <p>SnakeYAML は Spring Boot が application.yml 読み込みのために標準で依存しているため、 このパーサーのために新規依存を追加する必要はない。
 *
 * <p>期待するYAML構造:
 *
 * <pre>{@code
 * tables:
 *   products:
 *     - id: 1
 *       name: "商品A"
 *       price: 1000
 * mocks:
 *   - bean: productRepository
 *     method: findByIdAndDeletedFalse
 *     args: [1]
 *     returnType: java.util.Optional
 *     returns:
 *       id: 1
 *       name: "商品A"
 * }</pre>
 */
public class YamlTestDataParser implements TestDataParser {

    @SuppressWarnings("unchecked")
    @Override
    public TestDataSet parse(InputStream input) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(input);
        if (root == null) {
            return new TestDataSet(Map.of(), List.of());
        }

        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        Object tablesNode = root.get("tables");
        if (tablesNode instanceof Map<?, ?> tablesMap) {
            for (Map.Entry<?, ?> entry : tablesMap.entrySet()) {
                String tableName = String.valueOf(entry.getKey());
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Object row : (List<?>) entry.getValue()) {
                    rows.add((Map<String, Object>) row);
                }
                tables.put(tableName, rows);
            }
        }

        List<TestDataSet.MockStub> mocks = new ArrayList<>();
        Object mocksNode = root.get("mocks");
        if (mocksNode instanceof List<?> mocksList) {
            for (Object m : mocksList) {
                Map<String, Object> mockMap = (Map<String, Object>) m;
                mocks.add(
                        new TestDataSet.MockStub(
                                (String) mockMap.get("bean"),
                                (String) mockMap.get("method"),
                                (List<Object>) mockMap.getOrDefault("args", List.of()),
                                (String) mockMap.get("returnType"),
                                (Map<String, Object>) mockMap.get("returns")));
            }
        }

        return new TestDataSet(tables, mocks);
    }
}
