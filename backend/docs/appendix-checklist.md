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
- [ ] 新しいビジネス例外が `BusinessException` を継承している（`RuntimeException` 直継承は禁止）
- [ ] 例外メッセージが `messages.properties` 経由
