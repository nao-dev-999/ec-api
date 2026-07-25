package com.example.ecapi.testsupport.data;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link TestDataSet} を実際の DB / Mockito モックに反映するローダー。
 *
 * <p>テスト専用の Spring Bean として {@code @TestConfiguration} 等から登録して使う想定。 本番の依存グラフには含めない（src/test 配下に置く）。
 */
@Component
public class TestDataLoader {

    private static final Pattern ISO_INSTANT_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TestDataLoader(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** tables セクションの内容を INSERT 文にして DB へ投入する。 */
    public void loadTables(TestDataSet dataSet) {
        for (Map.Entry<String, List<Map<String, Object>>> table : dataSet.tables().entrySet()) {
            String tableName = table.getKey();
            for (Map<String, Object> row : table.getValue()) {
                insertRow(tableName, row);
            }
        }
    }

    private void insertRow(String tableName, Map<String, Object> row) {
        String columns = String.join(", ", row.keySet());
        String placeholders =
                row.keySet().stream().map(c -> ":" + c).reduce((a, b) -> a + ", " + b).orElse("");

        String sql = "INSERT INTO %s (%s) VALUES (%s)".formatted(tableName, columns, placeholders);
        jdbcTemplate.update(sql, new MapSqlParameterSource(coerceRowValues(row)));
    }

    /**
     * YAML から読み込んだ値は文字列のまま渡ってくるため、ISO-8601 の日時文字列は {@link OffsetDateTime} に変換してからバインドする。 変換しないと
     * VARCHAR としてバインドされ、timestamptz カラムへの暗黙キャストが Postgres 側で拒否される。{@code Instant} のままだと pgjdbc が
     * SQL 型を推測できないため、{@code OffsetDateTime} を使う。
     */
    private Map<String, Object> coerceRowValues(Map<String, Object> row) {
        Map<String, Object> coerced = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            coerced.put(entry.getKey(), coerceCellValue(entry.getValue()));
        }
        return coerced;
    }

    private Object coerceCellValue(Object value) {
        if (value instanceof String s && ISO_INSTANT_PATTERN.matcher(s).matches()) {
            return OffsetDateTime.parse(s);
        }
        return value;
    }

    /**
     * mocks セクションの内容を Mockito のスタブとして登録する。
     *
     * <p>リフレクションでモックの対象メソッドを「呼び出す」ことで Mockito の内部スタブ登録機構 （直前の呼び出しを記録する仕組み）に乗せている。これは Mockito
     * の公開APIを型消去越しに 利用するテクニックであり、動的なスタブ定義フレームワークでよく使われるパターン。
     */
    public void loadMocks(TestDataSet dataSet, ApplicationContext context) {
        for (TestDataSet.MockStub stub : dataSet.mocks()) {
            try {
                applyMockStub(stub, context);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("モックスタブの適用に失敗しました: " + stub, e);
            }
        }
    }

    private void applyMockStub(TestDataSet.MockStub stub, ApplicationContext context)
            throws ReflectiveOperationException {
        Object mockBean = context.getBean(stub.bean());

        Method method = findMethod(mockBean.getClass(), stub.method(), stub.args().size());
        Object[] args = coerceArgs(method, stub.args());

        Object returnValue = buildReturnValue(stub);

        // mockBean 上でメソッドを実行し、その呼び出しを Mockito.when() で捕捉する
        Object invocationResult = method.invoke(mockBean, args);
        Mockito.when(invocationResult).thenReturn(returnValue);
    }

    private Method findMethod(Class<?> mockClass, String methodName, int argCount) {
        for (Method method : mockClass.getMethods()) {
            // ジェネリクス消去に伴う合成ブリッジメソッド（例: findById(Object)）は実パラメータ型が
            // Object になり呼び出し時にキャスト例外を起こすため除外し、実メソッドのみを対象にする
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new IllegalArgumentException(
                "メソッドが見つかりません: %s (引数%d個)".formatted(methodName, argCount));
    }

    /**
     * YAML の数値はすべて Integer としてパースされるため、リフレクション呼び出し前に メソッドの実パラメータ型（Long など）へ変換する。変換しないと {@code
     * Method#invoke} が型不一致で失敗する。
     */
    private Object[] coerceArgs(Method method, List<Object> rawArgs) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[rawArgs.size()];
        for (int i = 0; i < rawArgs.size(); i++) {
            args[i] = coerceArgValue(rawArgs.get(i), paramTypes[i]);
        }
        return args;
    }

    private Object coerceArgValue(Object value, Class<?> targetType) {
        if (!(value instanceof Number number) || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return number.longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return number.intValue();
        }
        if (targetType == Short.class || targetType == short.class) {
            return number.shortValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return number.doubleValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return number.floatValue();
        }
        return value;
    }

    private Object buildReturnValue(TestDataSet.MockStub stub) {
        if (stub.returns() == null) {
            return null;
        }
        if ("java.util.Optional".equals(stub.returnType())) {
            // Optional<T> は returns の中身から中身の型を推測できないため、
            // 呼び出し側で returnType に "java.util.Optional:com.example.ecapi.entity.Product"
            // のように内包型を併記する運用にするのが実務的（サンプルでは Product 固定にしている）
            return Optional.of(
                    objectMapper.convertValue(
                            stub.returns(), com.example.ecapi.entity.Product.class));
        }
        try {
            Class<?> targetType = Class.forName(stub.returnType());
            return objectMapper.convertValue(stub.returns(), targetType);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("returnType が解決できません: " + stub.returnType(), e);
        }
    }

    /** テストで投入したテーブルをクリーンアップする（after each で呼ばれる）。 */
    public void cleanup(Iterable<String> tableNames) {
        for (String tableName : tableNames) {
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM " + tableName);
        }
    }
}
