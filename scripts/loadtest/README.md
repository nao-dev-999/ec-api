# 負荷試験用テストデータ生成

商品一覧・検索(80%)とカート→注文確定(20%)のスループット計測を想定した、
RDSステージング相当の環境に投入するテストデータを生成する。

外部ライブラリ(Faker/numpy等)は使わず、Python3標準ライブラリのみで動作する。

## 想定データ規模(5年分・デフォルト値)

| テーブル | 件数 | 補足 |
|---|---|---|
| category | 78 | 商品名の名詞プールと共通 |
| product | 100,000 | 5年間で線形に増加するcreated_at |
| product_category | 15万〜25万 | 商品ごとに1〜3カテゴリ |
| customer | 150,000 | 登録は直近ほど多い(事業成長を模した分布) |
| customer_order | 550,000 | 直近ほど多い。14日以上前の注文はDELIVERED/CANCELLEDに強制収束 |
| customer_order_detail | 約137万 | 平均2.5点/注文、商品はZipf分布で選択(人気商品に偏る) |
| review | 約19万(pool 40万 × sample率35%) | 実際に購入した(顧客,商品)ペアからのみ生成 |
| cart_item | 8,000 | 直近14日以内のみ(アクティブなカートを想定) |

商品の人気度はZipf分布(`--zipf-s`で調整、既定0.9)で決めており、一部の商品に注文・レビューが
偏って集中する(実際のECサイトのロングテール分布を再現するため)。

## 使い方

### 1. データ生成

```bash
python3 scripts/loadtest/generate_load_test_data.py \
  --out-dir scripts/loadtest/out \
  --products 100000 --customers 150000 --orders 550000
```

まずは小規模(`--products 500 --customers 300 --orders 800`程度)でドライランし、
`scripts/loadtest/out/SUMMARY.md` で生成結果を確認してから本番規模を流すことを推奨する。

主なオプション:

| オプション | 既定値 | 説明 |
|---|---|---|
| `--out-dir` | `scripts/loadtest/out` | CSV出力先 |
| `--products` / `--customers` / `--orders` | 100000 / 150000 / 550000 | 各テーブルの件数 |
| `--years` | 5 | タイムスタンプを分散させる過去年数 |
| `--zipf-s` | 0.9 | 商品人気度の偏り指数(大きいほど一部商品に集中) |
| `--seed` | 42 | 乱数シード(同じ値なら再現可能) |

実行時間の目安(このリポジトリの検証環境・PostgreSQL 16ローカルで計測): 商品3万件・注文6万件の
生成が数十秒程度。商品10万件・注文55万件規模ではその数倍〜10倍程度を見込む。

### 2. DBへの投入

`\copy`(psqlのクライアント側コピー)を使うため、RDSのようにサーバー側のファイルシステムに
アクセスできない環境でもそのまま使える。

```bash
PGHOST=<RDSエンドポイント> PGPORT=5432 PGDATABASE=ec_db PGUSER=<user> PGPASSWORD=<password> \
  ./scripts/loadtest/load_into_db.sh scripts/loadtest/out
```

投入後、投入済み最大IDまでシーケンスを再同期し、`ANALYZE`まで自動実行する(統計情報が古いままだと
負荷試験のクエリプランが実運用と乖離するため必須)。

投入は空のテーブルに対して行う想定。既存データが残っている環境に流す場合は、事前に対象テーブルを
`TRUNCATE ... RESTART IDENTITY CASCADE`しておくこと。

### 3. 負荷試験シナリオ側で使えるキーワード

`generate_load_test_data.py` 実行後の `SUMMARY.md` に、実際に生成されたデータでの
ヒット件数付きで出力される。目安:

- **大量ヒット**: `ワイヤレス` など(商品名の1〜2割に出現する形容詞)
- **少数ヒット**: カテゴリ名詞の中でも出現頻度の低いもの(数百件程度)
- **1件のみヒット**: どの商品にも末尾に付与される `[EC-0000001]` 形式の商品コード。
  任意の商品IDに対して `EC-{id:07d}` で1件ヒットするキーワードを組み立てられる
- **0件ヒット**: `ZZZ-LOADTEST-NOHIT`(意図的にどの商品にも出現させていない語)

## 既知の制約・スコープ外

- クーポン(coupon)・お気に入り(wishlist_item)・複数配送先(shipping_address)・決済(payment)
  テーブルは生成対象外。注文確定シナリオでの `coupon_code` は常に未設定で投入している
- 商品カテゴリは名詞プール(78種)をそのまま流用しており、それ以上の件数を`--categories`で
  指定すると `その他カテゴリNNN` という穴埋め名で不足分を補う(検索対象語としての意味は持たない)
- 開発用シード `backend/src/main/resources/db/seed/R__init_data.sql` とは独立した仕組み。
  そちらは現行スキーマ(V19で`deleted_at`→`is_deleted`へ変更後)と不整合があり投入に失敗する
  既知の問題があるため、本スクリプトはそれに依存しない
