# Gatling 負荷試験

商品一覧・検索(80%)とカート→注文確定(20%)のシナリオで負荷試験を行う。Scalaは使わず、
Gatling Java DSLで書いているため、このリポジトリの他モジュール(Java/Gradle)と同じ形で扱える。

シミュレーション本体: [`EcSiteSimulation.java`](src/gatling/java/com/example/ecapi/loadtest/EcSiteSimulation.java)

対象データは `scripts/loadtest/generate_load_test_data.py` で生成・投入したものを想定している
(顧客アカウントが `loadtest-customer-{連番7桁}@example.com` / `password123` の規則に従うため)。

## 実行方法

```bash
# ローカル(既定値: 10ユーザー、60秒)
./gradlew :gatling:gatlingRun -DbaseUrl=http://localhost:8080

# 投入したデータ件数に合わせる場合
./gradlew :gatling:gatlingRun \
  -DbaseUrl=http://localhost:8080 \
  -DproductCount=30000 -DcustomerCount=20000
```

HTMLレポートは `gatling/build/reports/gatling/<実行名>/index.html` に生成される。実行ログの最後に
`Reports generated, please open the following file: ...` としてパスが表示される。

## 主なオプション(`-D`で指定)

| オプション | 既定値 | 説明 |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | 試験対象のベースURL |
| `users` | 10 | 総仮想ユーザー数(商品一覧・検索80% / カート→注文確定20%に自動配分) |
| `rampSeconds` | 30 | ユーザーを起動しきるまでの時間 |
| `browseSeconds` | 60 | 商品一覧・検索シナリオを継続する時間 |
| `productCount` | 100000 | 投入済み商品件数(検索・カート対象のID範囲を合わせる) |
| `customerCount` | 150000 | 投入済み顧客件数(ログインアカウントのID範囲を合わせる) |
| `pauseMinMs` / `pauseMaxMs` | 1500 / 5000 | 商品一覧・検索シナリオの1アクションごとの待機時間(ミリ秒) |

`productCount`/`customerCount`は、`generate_load_test_data.py`実行時に指定した`--products`/
`--customers`と必ず一致させること(ずれると存在しないIDへのリクエストが増え、404やログイン失敗が
増える)。

## 重要: レートリミッターとの関係

このAPIには `RateLimitingFilter`(Bucket4j + Redis)があり、**未認証リクエストは同一IPから
1分あたり200回まで**(`GENERAL`ティア)に制限される。認証済みリクエスト(カート/注文/配送先住所)は
ユーザーID単位の制限なので、この上限には影響しない。

Gatlingを1台のマシンから実行する場合、商品一覧・検索(未認証)の全リクエストが同一IP扱いで合算
されるため、**`users`を増やしても実際のAPI/DB性能ではなくレートリミッターの上限(200req/分)を
測定してしまう**ことがある。既定値(`users=10`、`pauseMinMs=1500`〜`pauseMaxMs=5000`)は、
この上限を踏まないよう意図的に控えめに設定してある。

より高い負荷でAPI/DBそのものの性能を測りたい場合、次のいずれかを選ぶ必要がある。

1. **レートリミッターの制約込みで試験する**(実運用の防御込みの挙動を見る)
   → `users`を増やしても429が増えるだけなので、この目的には向かない
2. **試験対象環境(ステージング等)でのみレートリミッターの閾値を一時的に緩和する**
   → 本番環境の設定には影響させないこと
3. **複数の送信元IPからGatlingを実行する**(分散実行)
   → 本番同様、クライアントが分散している状態を再現できる

どの方針を取るかは試験の目的次第なので、実行前にチームで合意しておくこと。

## シナリオの設計メモ

- 商品一覧・検索(`browsingScenario`): 一覧取得・大量ヒット検索・1件のみヒット検索(商品コード)・
  0件ヒット検索・商品詳細をランダムに実行する。キーワードは
  `scripts/loadtest/README.md` に記載の設計と対応している
- カート→注文確定(`checkoutScenario`): ログイン→カート追加→カート確認→配送先住所取得
  (未登録なら新規登録)→注文確定、という一連の流れ
- 注文確定は「在庫不足による409(`INSUFFICIENT_STOCK`)」を正常な業務上のレスポンスとして許容している
  (生成データは一部商品を意図的に低在庫/在庫切れにしているため)
- 商品コード検索は、対象商品が論理削除済み(生成データの約3%)だと0件ヒットになるのも仕様通りの
  挙動として許容している

## 既知の制約

- クーポン適用のシナリオは含まない(`scripts/loadtest/generate_load_test_data.py`がクーポンデータを
  生成しないため)
- 管理画面(admin API)のシナリオは含まない
