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

冪等な操作（参照系、`Idempotency-Key` 付きの作成系）のみリトライ対象とする。リトライなしでは決済の二重実行等の事故につながるため、**リトライする場合は必ず冪等性が担保された呼び出しに限定する**（[16. 冪等性](16-idempotency.md)参照）。

```java
// ✅ Spring Retry を使う場合の例。リトライ対象は接続エラー・5xxのみ
@Retryable(
    retryFor = { ResourceAccessException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, multiplier = 2))
public PaymentResult charge(ChargeRequest request) { ... }
```

- 4xx（クライアントエラー）はリトライしない
- バックオフは指数的に増やす（固定間隔での連続リトライは外部サービスへの負荷集中を招く）

## サーキットブレーカー

外部サービスが継続的に障害を起こしている状態で呼び出しを繰り返さないよう、Resilience4jの `CircuitBreaker` を導入する。障害時のフォールバック（例: 決済不可時は注文を `PAYMENT_PENDING` 状態で保留する等）をビジネスロジックとして明確に決めてから実装する。

```java
@CircuitBreaker(name = "paymentGateway", fallbackMethod = "chargeFallback")
public PaymentResult charge(ChargeRequest request) { ... }

private PaymentResult chargeFallback(ChargeRequest request, Exception ex) {
    // ✅ 例外を握りつぶさず、呼び出し元が判断できる結果を返す
    throw new PaymentGatewayUnavailableException();
}
```

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
- [ ] 継続障害時のサーキットブレーカーとフォールバック挙動を定義している
- [ ] Webhook受信で署名検証をしている
- [ ] Webhook受信でイベントIDによる重複排除をしている
- [ ] Webhookのペイロードをログにそのまま出力していない
