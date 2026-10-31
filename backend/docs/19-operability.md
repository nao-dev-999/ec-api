# 19. 運用方式（Actuator公開・環境差分管理）

> [← インデックスに戻る](../コーディング規約.md)

---

## 1. Actuatorエンドポイントの公開方針

`spring-boot-starter-actuator` は導入済みで `/actuator/health` のみ `SecurityConfig` で `permitAll` にしているが、`management.endpoints.web.exposure.include` 等の明示設定がなく、Spring Bootのデフォルト（`health`・`info` のみ公開）に依存している状態。意図したデフォルトか、明示するかを以下の方針で統一する。

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      probes:
        enabled: true          # ECS/Kubernetesのliveness/readiness分離に対応
      show-details: when-authorized
  info:
    env:
      enabled: false            # 環境変数の内容をinfoエンドポイント経由で漏らさない
```

| エンドポイント | 公開範囲 | 理由 |
|---|---|---|
| `/actuator/health/liveness` | ALB/ECSヘルスチェックから到達可能・認証不要 | コンテナの生存確認のみを返す。DBやRedisの状態は含めない |
| `/actuator/health/readiness` | ECSのターゲットグループ登録確認用・認証不要 | DB/Redis接続性を含めた「トラフィックを受けてよいか」の判定に使う |
| `/actuator/health`（詳細） | `show-details: when-authorized` とし、認証済み管理者のみ詳細を見られるようにする | DB接続文字列の一部やコンポーネント構成が露出するリスクを避ける |
| `/actuator/metrics`, `/actuator/prometheus` | 外部（ALB経由）には公開しない。VPC内部からのみ到達可能にするか、専用の管理ポート(`management.server.port`)に分離する | メトリクスの外部公開は不要な情報開示になる |
| `/actuator/env`, `/actuator/beans`, `/actuator/mappings` 等 | 非公開(`exposure.include` に含めない) | アプリ内部構成の露出を避ける |

liveness/readinessをECSのヘルスチェックパスに正しく割り当てること（liveness=再起動判断、readiness=ALBへの登録判断）。両者を同じパスにすると、DB障害時にコンテナごと再起動を繰り返す「サッシングループ」を起こしうる。

## 2. 管理ポートの分離（推奨・導入は負荷状況に応じて判断）

Actuatorを業務トラフィックと別ポートに分離すると、ALBのリスナー設定だけでActuatorへの外部到達を遮断できる。

```yaml
management:
  server:
    port: 9090
```

ECS/ALB構成の変更を伴うため、導入タイミングはインフラ側（`infrastructure/terraform`）の変更とあわせて計画する。

## 3. 環境差分の一覧管理

`application.yml` にコメントとして知見は分散しているが、環境ごとの差分を一覧化したものがない。以下を実体のドキュメントとして持つ（値そのものはTerraform側が正、ここでは「何が環境で変わるか」の一覧のみを管理する）。

| 設定項目 | ローカル | CI | ステージング | 本番 |
|---|---|---|---|---|
| `APP_SWAGGER_ENABLED` | true | true | 要検討（関係者限定公開か） | false |
| `app.cors.allowed-origins` | `http://localhost:3000` | テスト用ダミー | ステージングフロントのオリジン | 本番フロントのオリジンのみ |
| `SPRING_DATA_REDIS_SSL_ENABLED` | false | false | true | true |
| `spring.jpa.hibernate.ddl-auto` | `validate`（マイグレーション必須） | `validate` | `validate` | `validate` |
| ログレベル | DEBUG可 | INFO | INFO | INFO（機微情報マスキング必須、[18. セキュリティ強化](18-security-hardening.md)参照） |
| `management.endpoints.web.exposure.include` | 広め（開発効率優先） | 広め | 絞る | 最小限 |

環境変数の実体（シークレット含む）の管理方法は `infrastructure/terraform` 側のSecrets Manager定義を正とし、本ドキュメントでは項目の一覧性のみを担保する。新しい環境依存設定を追加した場合は、この表に1行追加することをPRレビューの確認項目に含める。

---

## チェックリスト

- [ ] liveness/readinessのヘルスチェックパスを分け、ECS側の設定と整合させている
- [ ] `/actuator/metrics` 等の詳細エンドポイントを外部に公開していない
- [ ] 新しい環境依存設定を追加した場合、本ドキュメントの環境差分表を更新した
