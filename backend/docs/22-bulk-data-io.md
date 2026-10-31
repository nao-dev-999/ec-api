# 22. 一括データ入出力（CSV等）の方式

> [← インデックスに戻る](../コーディング規約.md)

---

管理画面向けの商品一括登録・注文データのCSVエクスポート等、大量データのAPI I/Oは現状未実装。必要になった際に設計が割れないよう、方式を先に定める。

## エクスポート（大量データのダウンロード）

### 同期ダウンロード vs 非同期ジョブ化の基準

| 想定件数・処理時間 | 方式 |
|---|---|
| 〜数千件・数秒以内に生成できる | 同期ストリーミングレスポンス |
| 数万件以上、または集計処理を伴い生成に時間がかかる | 非同期ジョブ化（生成を別スレッド/batchに回し、完了後にダウンロードURLを発行） |

```java
// ✅ 同期の場合: Spring MVCのStreamingResponseBodyで逐次書き出す
// (全件をメモリに載せてからCSVに変換しない)
@GetMapping(value = "/api/admin/orders/export", produces = "text/csv")
public ResponseEntity<StreamingResponseBody> exportOrders(
        @RequestParam LocalDate from, @RequestParam LocalDate to) {

    StreamingResponseBody body = outputStream -> {
        try (var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             var cursor = orderRepository.streamByDateRange(from, to)) {
            writer.write(CSV_HEADER);
            cursor.forEach(order -> writeCsvRow(writer, order));
        }
    };
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"orders.csv\"")
            .body(body);
}
```

- JPAの `Stream<T>`（`@QueryHints` + `Stream` 戻り値）やページング併用で、全件を一度にメモリへロードしない
- 件数に上限を設ける（例: `from`〜`to` の期間を最大92日に制限する等）。無制限のエクスポートはDB・メモリ双方の圧迫要因になる

### 非同期ジョブ化する場合

```
1. POST /api/admin/orders/export-jobs  → ジョブを登録し、ジョブIDを返す(202 Accepted)
2. GET  /api/admin/export-jobs/{jobId} → ステータス確認(PENDING/PROCESSING/COMPLETED/FAILED)
3. 完了後、レスポンスにS3署名付きGET URL(有効期限付き)を含める
```

生成処理はAPIプロセス内の `@Async` ではなく `batch` モジュール、またはキュー経由のワーカーに任せる（[20. 非同期処理](20-async-events.md)の判断基準に従う: 失敗時にリトライが必要なため）。

## インポート（CSV一括登録等）

```
POST /api/admin/products/import
Content-Type: multipart/form-data
```

- ファイルサイズ上限を設ける（[18. セキュリティ強化](18-security-hardening.md)の `multipart.max-file-size` に準拠)
- **全件成功 or 全件失敗(トランザクション一括)にするか、行単位で成否を分けるか**を事前に決める。ECサイトの在庫・価格データは「一部だけ反映されて気づかない」事故の影響が大きいため、**原則バリデーションエラーが1行でもあれば全件を登録せず、エラー一覧を返す**方式を既定とする
- レスポンスでは成功件数とは別に、**行番号付きのエラー一覧**を返す

```json
{
  "code": "IMPORT_VALIDATION_ERROR",
  "message": "一部の行でエラーがあります。1件も登録されていません。",
  "rowErrors": [
    { "row": 3, "field": "price", "message": "0以上の値を入力してください" },
    { "row": 12, "field": "name", "message": "必須項目です" }
  ]
}
```

- 文字コードは UTF-8(BOM付き) を既定とする。Excelで開いた際の文字化け対策として、エクスポート時はBOMを付与する
- 件数が多い場合（数千行以上）はインポートも非同期ジョブ化し、結果を後から確認できるようにする

---

## チェックリスト

- [ ] エクスポートのメモリ使用量が件数に比例して増大しない（ストリーミング or ページング取得にしている）
- [ ] エクスポート対象期間・件数に上限を設けている
- [ ] 大量データの生成・取り込みを `@Async` ではなく batch/キューに任せている
- [ ] インポートの成否方針（全件ロールバック or 行単位）を決め、エラー時に行番号付きで返している
- [ ] CSVの文字コード(UTF-8 BOM)をエクスポート/インポート双方で統一している
