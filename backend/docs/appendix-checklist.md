# 付録: PR レビュー前チェックリスト

> [← インデックスに戻る](../コーディング規約.md)

---

- [ ] `./gradlew spotlessCheck` がパス
- [ ] `./gradlew test` がパス
- [ ] 新しいエンドポイントに認可アノテーションが付いている
- [ ] 新しい Flyway マイグレーションファイルのバージョンが連番
- [ ] エンティティに `@Builder` が付いていない
- [ ] Service が `SecurityContextHolder` を参照していない
- [ ] `createdBy` / `updatedBy` を手動セットしていない（`AuditorAware` に任せる）
- [ ] 在庫・ステータス遷移等の更新処理でクライアントから `version` を受け取っている
- [ ] 読み取りメソッドに `@Transactional(readOnly = true)` が付いている
- [ ] 新しいビジネス例外が `ErrorCode` のステータスに対応する中間抽象クラス（`BadRequestException` / `ResourceNotFoundException` / `ConflictException`）を継承している（`RuntimeException`・`BusinessException` 直継承は禁止）
- [ ] 例外メッセージが `messages.properties` 経由（`ErrorCode` に定数とメッセージキーを追加済み）
- [ ] 更新処理でクライアントの `version` を `OptimisticLockConflictException.throwIfStale()` で比較している（`setVersion()` は効かない）
- [ ] 楽観ロックなど Spring が変換する例外は、変換後の型（例: `ObjectOptimisticLockingFailureException`）でテストしている
- [ ] 新しいエンドポイントがレスポンス構造を破壊的に変更する場合、[14. APIバージョニング](14-api-versioning.md)の方針に沿っている
- [ ] 外部APIを呼び出す処理にタイムアウトを設定している（[15. 外部API連携](15-external-integration.md)）
- [ ] 金銭が絡む作成系エンドポイントに `Idempotency-Key` を実装している（[16. 冪等性](16-idempotency.md)）
- [ ] DTOの `String`・配列フィールドに `@Size(max = ...)` を付与している（[18. セキュリティ強化](18-security-hardening.md)）
- [ ] 新しいエンドポイントに `@Operation`（summary）を付与している（[21. OpenAPIドキュメント化](21-api-documentation.md)）
