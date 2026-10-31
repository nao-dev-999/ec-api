# 15. 外部API連携（決済・配送等）

> [← インデックスに戻る](../コーディング規約.md)

---

`batch` モジュールに決済関連のDTO（`PaymentSettlementRow` 等）が存在するが、現時点で `backend` に外部決済代行・配送業者等の外部API呼び出し実装は存在しない。
連携が必要になった際に毎回方式が揺れないよう、導入前に方針を定めておく。

## HTTPクライアント

`RestClient`（Spring 6.1〜の同期クライアント）を標準とする。`RestTemplate` は非推奨のため新規実装では使わない。仮想スレッドを使う本プロジェクトでは非同期専用の `WebClient` を無理に導入する必要はない。

```java
// ✅ RestClientをBean化し、外部サービスごとに専用クライアントを用意する
@Configuration
public class PaymentClientConfig {

    @Bean
    public RestClient paymentRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(paymentGatewayProperties.baseUrl())
                .requestFactory(ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(3))
                        .withReadTimeout(Duration.ofSeconds(10))
                        .toRequestFactory())
                .build();
    }
}
```

- 外部サービスごとに個別のBeanを定義する（タイムアウト・ベースURL・認証方式が異なるため、共有クライアントにしない）
- 接続先の認証情報（APIキー・シークレット）は環境変数経由で注入し、コードにハードコードしない（既存の `SPRING_DATA_REDIS_PASSWORD` と同様の扱い）

## タイムアウト

| 区間 | 目安 | 理由 |
|---|---|---|
| クライアント接続（connect timeout） | 3秒 | 接続自体が確立しない場合に早期に諦める |
| 読み取り（read timeout） | 外部サービスのSLAに応じて設定（決済系は10秒程度） | 無限待ちでスレッド（キャリアスレッド含む）を占有しない |
| API全体（ユーザー向けレスポンス） | `server.tomcat.keep-alive-timeout`・ALBの `idle_timeout`（60秒）を超えない範囲 | 上流でタイムアウトする前に自分で制御する |

外部呼び出しのタイムアウト値は、必ず自分より上流（ALB・ブラウザ）のタイムアウトより短く設定する。

## リトライ

冪等な操作（参照系、`Idempotency-Key` 付きの作成系）のみリトライ対象とする。5xxやタイムアウトでも外部サービス側では処理が完了している可能性があり、冪等でない呼び出しを再送すると決済の二重実行等の事故につながるため、**リトライする場合は必ず冪等性が担保された呼び出しに限定する**（[16. 冪等性](16-idempotency.md)参照）。

Spring Framework 7 に組み込まれた `@Retryable`（`org.springframework.resilience.annotation`）を標準とし、Spring Retry・Resilience4j 等の外部ライブラリは追加しない。設定クラスに `@EnableResilientMethods` を付与して有効化する。

```java
// ✅ リトライ対象は接続エラー・タイムアウト・5xxのみ
@Retryable(
    includes = { ResourceAccessException.class, HttpServerErrorException.class },
    maxRetries = 3,
    delay = 500,
    multiplier = 2,
    jitter = 100)
public PaymentResult charge(ChargeRequest request) { ... }
```

| 応答 | 扱い | 理由 |
|---|---|---|
| 接続エラー・タイムアウト・5xx | リトライする | サーバー側の一時的な不調で、時間をおけば成功する可能性がある |
| 4xx（下記を除く） | リトライしない | リクエスト内容の問題であり、同じリクエストを再送しても結果は変わらない |
| 401（トークン期限切れ） | トークンを再取得して1回だけ再送 | 認証ヘッダーが変わるため再送に意味がある。`@Retryable` ではなく `ClientHttpRequestInterceptor` で実装する |
| 429 | `Retry-After` の秒数だけ待ってリトライ | レート制限による一時的な拒否のため |

- バックオフは指数的に増やし、ジッターを入れる（固定間隔・一斉の再送は障害中の外部サービスへの負荷集中を招く）
- 回数は3回程度までに抑える。数秒で回復しない継続障害には効かず、負荷を増やす側に回るため

## 継続障害への備え

サーキットブレーカー（Resilience4j等）は導入しない。外部サービスの継続障害に自システムが巻き込まれないよう、以下で最低限の防御を行う。

- **タイムアウトを短く保つ**: 障害時に待たされる時間の上限になる（[タイムアウト](#タイムアウト)参照）
- **リトライ回数を絞る**: 上記のとおり3回程度まで
- **同時実行数を制限する（任意）**: Spring Framework 7 の `@ConcurrencyLimit` で外部呼び出しの並列数に上限を設け、外部サービスの遅延時に仮想スレッドが際限なく滞留するのを防ぐ

```java
@ConcurrencyLimit(20)
@Retryable(includes = { ResourceAccessException.class, HttpServerErrorException.class })
public PaymentResult charge(ChargeRequest request) { ... }
```

リトライし尽くしても失敗した場合の業務上の扱い（例: 決済不可時はエラーで注文を受け付けないのか、`PAYMENT_PENDING` 状態で保留して後から決済するのか）は、実装前にビジネスロジックとして明確に決める。例外を握りつぶさず、呼び出し元が判断できる例外（例: `PaymentGatewayUnavailableException`）を送出する。

## Webhook受信（決済代行からのコールバック等）

```
POST /api/webhooks/payment
```

- **署名検証を必須とする**: 外部サービスが付与する署名ヘッダー（例: `X-Signature`）をシークレットキーで検証し、検証に失敗したリクエストは401で即座に拒否する。送信元IPの制限だけに頼らない
- **重複配信への対応**: 外部サービスは同一イベントを複数回送ってくる前提で設計する。イベントID（外部サービス発行）をキーに処理済みかどうかをRedis/DBで判定し、二重処理を防ぐ
- **応答は速やかに返す**: Webhook受信後の重い処理（在庫更新・メール送信等）は受信処理と分離し、受信自体は200を即座に返す。外部サービス側のタイムアウトでリトライの嵐を招かないため
- **ペイロードはログに出さない**: カード情報等の機微情報が含まれる場合があるため、[12. ロギング](12-logging.md)のマスキング方針に従う

```java
@PostMapping("/api/webhooks/payment")
public ResponseEntity<Void> handlePaymentWebhook(
        @RequestHeader("X-Signature") String signature,
        @RequestBody String rawPayload) {

    if (!signatureVerifier.isValid(rawPayload, signature)) {
        throw new InvalidWebhookSignatureException(); // 401
    }
    // ✅ 受信と処理を分離する。ここでは受理のみ行い、詳細処理は非同期に回す
    paymentWebhookService.enqueue(rawPayload);
    return ResponseEntity.ok().build();
}
```

---

## チェックリスト

- [ ] 外部サービスごとに個別の `RestClient` Beanを定義し、connect/readタイムアウトを設定している
- [ ] タイムアウト値が上流（ALB等）のタイムアウトより短い
- [ ] リトライ対象を冪等な操作に限定している
- [ ] リトライ対象を接続エラー・タイムアウト・5xxに絞り、4xxはリトライしていない（401・429は例外の扱いに従う）
- [ ] リトライし尽くした後の業務上の扱い（エラー／保留等）を定義している
- [ ] Webhook受信で署名検証をしている
- [ ] Webhook受信でイベントIDによる重複排除をしている
- [ ] Webhookのペイロードをログにそのまま出力していない
