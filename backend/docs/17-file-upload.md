# 17. ファイルアップロード方式

> [← インデックスに戻る](../コーディング規約.md)

---

現状、商品の画像は `imageUrl`（文字列URL）として扱われており、実際のファイルアップロードAPIは存在しない。管理画面から画像登録ができるようにする際の方式を事前に定める。

## 方式: S3署名付きURL方式（サーバを画像バイナリの中継点にしない）

```
1. クライアント: POST /api/admin/products/{id}/images/upload-url でアップロード先URLを要求
2. サーバ:        S3の署名付きURL（PUT用、有効期限付き）を発行して返す
3. クライアント: 署名付きURLに対して直接ファイルをPUTする（APIサーバを経由しない）
4. クライアント: アップロード完了後、POST /api/admin/products/{id}/images で
                 S3オブジェクトキーをサーバに登録する
```

APIサーバ自体にmultipartでファイルを受けて中継する方式（下記）は、大きなファイルでAPIサーバのスレッド・メモリを圧迫し、サーバのスケーリングと画像配信のスケーリングが結合してしまうため避ける。

```java
// ❌ 避ける: APIサーバがファイル本体を受け取ってS3にアップロードする中継方式
@PostMapping("/api/admin/products/{id}/images")
public ResponseEntity<Void> upload(@PathVariable Long id, @RequestParam MultipartFile file) {
    s3Client.putObject(..., file.getInputStream()); // サーバがバイナリ転送を肩代わりする
}
```

### 署名付きURL発行API

```java
// ✅ サーバはメタデータの発行のみを行い、バイナリ転送には関与しない
@PostMapping("/api/admin/products/{productId}/images/upload-url")
public ResponseEntity<UploadUrlResponse> issueUploadUrl(
        @PathVariable Long productId,
        @Valid @RequestBody UploadUrlRequest request) {

    // リクエストで拡張子・Content-Typeを受け取り、許可リストで検証する
    productImageService.validateContentType(request.contentType());

    String objectKey = "products/%d/%s".formatted(productId, UUID.randomUUID());
    URL uploadUrl = s3Presigner.presignPutObject(objectKey, request.contentType(), Duration.ofMinutes(5));

    return ResponseEntity.ok(new UploadUrlResponse(uploadUrl.toString(), objectKey));
}
```

## 制約・バリデーション

| 項目 | 方針 |
|---|---|
| 許可する拡張子/Content-Type | ホワイトリスト方式（`image/jpeg`, `image/png`, `image/webp` のみ等）。クライアント申告のContent-Typeを鵜呑みにせず、登録完了時にサーバ側でS3オブジェクトの実際のContent-Typeを検証する |
| ファイルサイズ上限 | 署名付きURLの発行時に `Content-Length-Range` 条件を付与し、S3側で上限超過を拒否する（例: 5MB） |
| 署名付きURLの有効期限 | 短時間（5〜10分程度）。長時間有効にすると漏洩時のリスクが増す |
| アップロード先バケット | 公開バケットと非公開バケットを分離する。商品画像は配信用CDN経由の公開バケット、本人確認書類等の機微なファイルは非公開バケット+都度署名付きGET URLで配信する |
| ウイルススキャン | 機微なファイル種別（本人確認書類等）を扱う場合、S3イベント経由でスキャンする仕組みを別途検討する（商品画像のみなら必須ではない） |

## やむを得ずAPIサーバ経由にする場合（小規模・低頻度）

社内向け管理画面でファイルサイズが小さく頻度も低い場合に限り、`MultipartFile` による直接アップロードを許容してもよい。その場合も以下を守る。

```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

- Content-Typeをサーバ側で再検証する（拡張子偽装対策）
- 仮想スレッド環境でも同期I/O処理となるため、頻度・同時実行数が高い用途では署名付きURL方式に切り替える

---

## チェックリスト

- [ ] 頻度・サイズの大きいアップロードはサーバ中継ではなく署名付きURL方式にしている
- [ ] Content-Type・拡張子をホワイトリストで検証している（クライアント申告を鵜呑みにしない）
- [ ] アップロードサイズに上限がある
- [ ] 署名付きURLの有効期限が短時間に設定されている
- [ ] 機微なファイルと公開ファイルでバケット・配信経路を分離している
