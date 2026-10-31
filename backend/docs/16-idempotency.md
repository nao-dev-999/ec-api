# 16. 冪等性（Idempotency-Key）の実装方式

> [← インデックスに戻る](../コーディング規約.md)

---

[API_INTERFACE_CONVENTIONS.md の「10. 冪等性」](../API_INTERFACE_CONVENTIONS.md#10-冪等性)で規約としては定義済みだが、実装がまだ存在しない。
注文作成・決済等、重複実行が金銭的被害につながるエンドポイントを実装する際の具体的な実装方式を定める。

## 対象エンドポイント

- `POST /api/orders`（注文作成）
- 将来追加される決済系エンドポイント（[15. 外部API連携](15-external-integration.md)）

一覧取得・検索等の参照系、削除（既に冪等）には適用しない。

## リクエスト

```
POST /api/orders
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

- クライアント（フロントエンド）がUUIDv4を生成してヘッダーに付与する
- ヘッダー未指定の場合、対象エンドポイントでは400を返す（必須ヘッダーとして扱う）

## サーバ側の処理フロー

Redisをキーストアとして使う（セッション・レート制限と同じRedisインスタンスを利用し、新規ミドルウェアを増やさない）。

```java
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor {

    private static final Duration TTL = Duration.ofHours(24);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T execute(String idempotencyKey, Class<T> responseType, Supplier<T> action) {
        String redisKey = "idempotency:" + idempotencyKey;

        // ✅ SETNXで「処理中」を先に書き込み、同時到達した2リクエストの競合を防ぐ
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", TTL);

        if (Boolean.FALSE.equals(acquired)) {
            String stored = redisTemplate.opsForValue().get(redisKey);
            if ("PROCESSING".equals(stored)) {
                // ✅ 同一キーが処理中に再送された場合は409を返す(クライアントは後で再試行する)
                throw new IdempotencyRequestInProgressException();
            }
            // ✅ 処理済みなら保存済みレスポンスをそのまま返す(新規作成しない)
            return readStoredResponse(stored, responseType);
        }

        try {
            T result = action.get();
            redisTemplate.opsForValue().set(redisKey, serialize(result), TTL);
            return result;
        } catch (RuntimeException ex) {
            // ✅ 業務エラーで失敗した場合はキーを解放し、同じキーでの再試行を許可する
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }
}
```

### 設計上のポイント

| 項目 | 方針 | 理由 |
|---|---|---|
| キー保存先 | Redis（`SETNX` + TTL） | レート制限と同じ基盤を再利用でき、複数インスタンス間で共有できる |
| TTL | 24時間 | クライアントの再送が現実的に発生しうる期間をカバーしつつ、無限に残さない |
| 同時到達の扱い | `SETNX` で先着のみ処理を継続、後着は409 | DBの一意制約と同様、Redis側でも競合を早期に検知する |
| ビジネスエラー時 | キーを解放する | バリデーションエラー等で失敗した場合、同じキーで正しい内容で再試行できるようにする |
| 成功時のレスポンス再生 | 保存したレスポンスをそのまま返す | 2回目の呼び出しで新しい注文が作られることを防ぐ（本来の目的） |

### 適用方法

Controller層にAOP（`@Idempotent` アノテーション + `Aspect`）で横断的に適用し、個々のController実装にRedis操作を書かせない。

```java
@Idempotent
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody OrderRequest request) {
    // ✅ Controllerはビジネスロジックの呼び出しのみ。冪等性制御はAspectが担う
    return ResponseEntity.status(HttpStatus.CREATED).body(orderApiMapper.toResponse(
            orderService.create(orderApiMapper.toCreateOrder(request))));
}
```

---

## チェックリスト

- [ ] 金銭が絡む作成系エンドポイントに `Idempotency-Key` を必須ヘッダーとして要求している
- [ ] 同時到達時に片方のみ処理されるよう `SETNX` 相当の排他制御を行っている
- [ ] 成功レスポンスを再送し、ビジネスエラー時はキーを解放している
- [ ] キーにTTLを設定し、Redisに無期限で残さない
