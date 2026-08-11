package com.example.ecapi.loadtest;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * 商品一覧・検索(80%)とカート→注文確定(20%)のシナリオを想定した負荷試験。
 *
 * <p>対象は scripts/loadtest/generate_load_test_data.py で投入したテストデータ。 顧客アカウントは
 * loadtest-customer-{連番7桁}@example.com / password123 の規則で生成されるため、 顧客件数さえ合わせればログイン情報をCSVで持つ必要がない。
 *
 * <p>注意: {@code RateLimitingFilter} により、未認証リクエストは同一IPから1分あたり200回まで
 * (GENERALティア)に制限される。Gatlingを1台のマシンから実行すると商品一覧・検索の全リクエストが
 * 同一IP扱いになるため、既定値は429を極力踏まないペースに抑えてある。より高い負荷を試したい場合、 レートリミッターの制約込みで試験するか、試験対象環境でのみ閾値を緩和するかを判断すること
 * (詳細は gatling/README.md 参照)。
 *
 * <p>実行例:
 *
 * <pre>
 * ./gradlew :gatling:gatlingRun \
 *   -DbaseUrl=https://staging.example.com \
 *   -Dusers=200 -DrampSeconds=60 -DbrowseSeconds=180 \
 *   -DproductCount=100000 -DcustomerCount=150000
 * </pre>
 *
 * HTMLレポートは gatling/build/reports/gatling/&lt;実行名&gt;/index.html に生成される。
 */
public class EcSiteSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    // 既定値は「RateLimitingFilter(未認証IP単位で200req/分)を踏まない」ことを優先した控えめな値。
    // Gatlingを1台のマシンから動かす限り、商品一覧・検索(未認証)の合計リクエスト数は台数に関わらず
    // 同一IP扱いで合算されるため、usersだけ増やしても429が増えるだけで実効スループットは頭打ちになる。
    // レートリミッターの制約込みで試験するか、試験対象環境でのみ閾値を緩和するかは
    // gatling/README.md を参照して判断すること。
    private static final int TOTAL_USERS = Integer.getInteger("users", 10);
    private static final int RAMP_SECONDS = Integer.getInteger("rampSeconds", 30);
    private static final int BROWSE_SECONDS = Integer.getInteger("browseSeconds", 60);
    private static final int PRODUCT_COUNT = Integer.getInteger("productCount", 100_000);
    private static final int CUSTOMER_COUNT = Integer.getInteger("customerCount", 150_000);
    private static final int PAUSE_MIN_MS = Integer.getInteger("pauseMinMs", 1500);
    private static final int PAUSE_MAX_MS = Integer.getInteger("pauseMaxMs", 5000);
    private static final String CUSTOMER_PASSWORD = "password123";
    private static final String ZERO_HIT_KEYWORD = "ZZZ-LOADTEST-NOHIT";
    private static final String COMMON_KEYWORD = "ワイヤレス";

    // 商品一覧・検索(80%)/ カート→注文確定(20%) のユーザー数配分。
    private static final int BROWSE_USERS = Math.round(TOTAL_USERS * 0.8f);
    private static final int CHECKOUT_USERS = TOTAL_USERS - BROWSE_USERS;

    private final HttpProtocolBuilder httpProtocol =
            http.baseUrl(BASE_URL)
                    .acceptHeader("application/json")
                    .contentTypeHeader("application/json")
                    .userAgentHeader("ec-api-gatling-loadtest");

    private static Iterator<Map<String, Object>> randomProductIdFeeder() {
        return Stream.generate(
                        (java.util.function.Supplier<Map<String, Object>>)
                                () ->
                                        Map.of(
                                                "productId",
                                                (long)
                                                        ThreadLocalRandom.current()
                                                                .nextInt(1, PRODUCT_COUNT + 1),
                                                "productCode",
                                                "EC-%07d"
                                                        .formatted(
                                                                ThreadLocalRandom.current()
                                                                        .nextInt(
                                                                                1,
                                                                                PRODUCT_COUNT
                                                                                        + 1))))
                .iterator();
    }

    private static Iterator<Map<String, Object>> randomCustomerFeeder() {
        return Stream.generate(
                        (java.util.function.Supplier<Map<String, Object>>)
                                () -> {
                                    int idx =
                                            ThreadLocalRandom.current()
                                                    .nextInt(1, CUSTOMER_COUNT + 1);
                                    return Map.of(
                                            "email",
                                            "loadtest-customer-%07d@example.com".formatted(idx),
                                            "password",
                                            CUSTOMER_PASSWORD);
                                })
                .iterator();
    }

    // ── 商品一覧・検索(80%): 検索ヒット件数の粗密を意図的に混在させる ──────────────

    private final ChainBuilder browseList =
            exec(
                    http("商品一覧")
                            .get("/api/customer/products?size=20")
                            .check(status().is(200))
                            .check(jsonPath("$.content").exists()));

    private final ChainBuilder searchCommonKeyword =
            exec(
                    http("商品検索(大量ヒット)")
                            .get("/api/customer/products?name=" + COMMON_KEYWORD + "&size=20")
                            .check(status().is(200)));

    // ランダムに選んだ商品コードが論理削除済み(生成データの約3%)の場合、検索から除外され
    // totalElements=0 になるのが仕様通りの挙動のため、0/1 のどちらも許容する。
    private final ChainBuilder searchExactCode =
            feed(randomProductIdFeeder())
                    .exec(
                            http("商品検索(1件のみヒット)")
                                    .get("/api/customer/products?name=#{productCode}")
                                    .check(status().is(200))
                                    .check(jsonPath("$.totalElements").in("0", "1")));

    private final ChainBuilder searchZeroHit =
            exec(
                    http("商品検索(0件ヒット)")
                            .get("/api/customer/products?name=" + ZERO_HIT_KEYWORD)
                            .check(status().is(200))
                            .check(jsonPath("$.totalElements").is("0")));

    private final ChainBuilder viewDetail =
            feed(randomProductIdFeeder())
                    .exec(
                            http("商品詳細")
                                    .get("/api/customer/products/#{productId}")
                                    .check(status().in(200, 404)));

    private final ScenarioBuilder browsingScenario =
            scenario("商品一覧・検索")
                    .during(Duration.ofSeconds(BROWSE_SECONDS))
                    .on(
                            randomSwitch()
                                    .on(
                                            percent(45.0).then(browseList),
                                            percent(20.0).then(searchCommonKeyword),
                                            percent(15.0).then(searchExactCode),
                                            percent(5.0).then(searchZeroHit),
                                            percent(15.0).then(viewDetail))
                                    .pause(
                                            Duration.ofMillis(PAUSE_MIN_MS),
                                            Duration.ofMillis(PAUSE_MAX_MS)));

    // ── カート→注文確定(20%) ────────────────────────────────────────────────

    private final ChainBuilder login =
            feed(randomCustomerFeeder())
                    .exec(
                            http("ログイン")
                                    .post("/api/customer/auth/login")
                                    .body(
                                            StringBody(
                                                    "{\"email\":\"#{email}\",\"password\":\"#{password}\"}"))
                                    .check(status().is(200)));

    private final ChainBuilder addToCart =
            feed(randomProductIdFeeder())
                    .exec(
                            session ->
                                    session.set(
                                            "quantity", ThreadLocalRandom.current().nextInt(1, 4)))
                    .exec(
                            http("カートに追加")
                                    .post("/api/customer/cart/items")
                                    .body(
                                            StringBody(
                                                    "{\"productId\":#{productId},\"quantity\":#{quantity}}"))
                                    .check(status().is(201))
                                    .check(jsonPath("$.productId").saveAs("lastAddedProductId"))
                                    .check(jsonPath("$.quantity").saveAs("lastAddedQuantity")));

    private final ChainBuilder viewCart =
            exec(http("カート確認").get("/api/customer/cart").check(status().is(200)));

    private final ChainBuilder ensureShippingAddress =
            exec(http("配送先住所一覧取得")
                            .get("/api/customer/shipping-addresses")
                            .check(status().is(200))
                            .check(
                                    jsonPath("$[0].id")
                                            .optional()
                                            .saveAs("existingShippingAddressId")))
                    .doIfOrElse(session -> session.contains("existingShippingAddressId"))
                    .then(
                            exec(
                                    session ->
                                            session.set(
                                                    "shippingAddressId",
                                                    session.getString(
                                                            "existingShippingAddressId"))))
                    .orElse(
                            exec(
                                    http("配送先住所登録")
                                            .post("/api/customer/shipping-addresses")
                                            .body(
                                                    StringBody(
                                                            """
                                                            {"recipientName":"負荷試験 太郎","postalCode":"100-0001",\
                                                            "prefecture":"東京都","city":"千代田区",\
                                                            "addressLine1":"1-2-3","phoneNumber":"090-1234-5678",\
                                                            "isDefault":true}"""))
                                            .check(status().is(201))
                                            .check(jsonPath("$.id").saveAs("shippingAddressId"))));

    // ランダムに選んだ商品の在庫が要求数を下回っている場合、注文確定は 409(INSUFFICIENT_STOCK)を
    // 返すのが仕様通りの挙動(生成データは一部商品を意図的に低在庫/在庫切れにしている)。
    // そのため 201(成功)/409(在庫不足での正常な拒否)の両方を許容する。
    private final ChainBuilder placeOrder =
            exec(
                    http("注文確定")
                            .post("/api/orders")
                            .body(
                                    StringBody(
                                            session ->
                                                    """
                                                    {"items":[{"productId":%s,"quantity":%s}],\
                                                    "shippingAddressId":%s}"""
                                                            .formatted(
                                                                    session.getString(
                                                                            "lastAddedProductId"),
                                                                    session.getString(
                                                                            "lastAddedQuantity"),
                                                                    session.getString(
                                                                            "shippingAddressId"))))
                            .check(status().in(201, 409)));

    private final ScenarioBuilder checkoutScenario =
            scenario("カート→注文確定")
                    .exec(login)
                    .pause(Duration.ofMillis(200), Duration.ofSeconds(1))
                    .exec(addToCart)
                    .pause(Duration.ofMillis(200), Duration.ofSeconds(1))
                    .exec(viewCart)
                    .exec(ensureShippingAddress)
                    .pause(Duration.ofMillis(200), Duration.ofSeconds(1))
                    .exec(placeOrder);

    {
        setUp(
                        browsingScenario.injectOpen(
                                rampUsers(BROWSE_USERS).during(Duration.ofSeconds(RAMP_SECONDS))),
                        checkoutScenario.injectOpen(
                                rampUsers(CHECKOUT_USERS).during(Duration.ofSeconds(RAMP_SECONDS))))
                .protocols(httpProtocol)
                .assertions(
                        global().successfulRequests().percent().gte(95.0),
                        global().responseTime().percentile(95.0).lt(2000));
    }
}
