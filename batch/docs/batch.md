# 14. バッチ処理

> [← インデックスに戻る](../../backend/コーディング規約.md)
> 対象スタック: Java 25 / Spring Boot 4.1 / Spring Batch / Hibernate (Spring Data JPA) / PostgreSQL

---

## 目次

- [14.2 想定ユースケース：日次売上集計バッチ](#142-想定ユースケース日次売上集計バッチ)
- [14.3 ジョブネット設計（外部I/F連携）](#143-ジョブネット設計外部if連携)
- [14.4 ジョブ・ステップの分割原則](#144-ジョブステップの分割原則)
- [14.5 分散・並列処理（パーティショニング）](#145-分散並列処理パーティショニング)
- [14.6 非機能要件](#146-非機能要件)
- [14.7 Hibernate関連の考慮事項](#147-hibernate関連の考慮事項)
- [14.8 リスタート・冪等性設計](#148-リスタート冪等性設計)
- [14.9 チェックリスト](#149-チェックリスト)

---

## 14.2 想定ユースケース：日次売上集計バッチ

### 概要

前日分の `CustomerOrder` / `CustomerOrderDetail` を集計し、商品別・カテゴリ別・顧客別の売上サマリテーブルを作成する。管理画面のダッシュボード表示用に事前計算しておくことで、都度JOIN集計による負荷を避ける。

### 対象データと件数設計（ピーク日想定）

| データ | 想定件数 | 備考 |
|---|---|---|
| `CustomerOrder`（当日発生の注文） | 約20万件 | 1注文あたり平均10明細と仮定 |
| `CustomerOrderDetail`（当日発生の明細） | **約200万件** | バッチの主なスキャン対象・chunk処理の母数 |
| `Product`（SKU数） | 約5万件 | 商品別集計のグルーピングキー数 |
| `Customer`（顧客数） | 約50万人 | 顧客別集計のグルーピングキー数（パーティションキー候補） |

### 出力（集計結果）の件数

| 出力テーブル | 粒度 | 想定行数（1日分） |
|---|---|---|
| `daily_sales_summary_by_product` | 商品別 | 動きがあった商品のみ：数千〜1万行程度 |
| `daily_sales_summary_by_category` | カテゴリ別 | 数十〜数百行 |
| `daily_sales_summary_by_customer` | 顧客別 | 当日注文した顧客のみ：最大20万行 |

---

## 14.3 ジョブネット設計（外部I/F連携）

バッチ処理は単独の集計処理として閉じず、**「どこからデータを受け取り、どこにデータを渡すか」を含めたジョブネット**として設計する。

```
[外部システムA]                                    [外部システムB]
 (例: 決済代行/倉庫システム)                        (例: 会計システム/DWH)
      │ 決済確定ファイル                                    ▲
      │ (SFTP/S3配置)                                       │ 集計結果ファイル
      ▼                                                     │
┌───────────────────────────────────────────────────────┐
│                     日次売上集計ジョブネット                │
│                                                           │
│  JobA: 受信I/F取込                                        │
│    ① キックファイル(到着フラグ)の存在確認・待機              │
│    ② ファイルフォーマット検証                               │
│    ③ ステージングテーブルへ取込                             │
│         │                                                 │
│         ▼                                                 │
│  JobB: 集計処理（Local Partitioningで並列化）               │
│    ① CustomerOrderDetail + ステージングデータを突合          │
│    ② 商品別・顧客別・カテゴリ別に集計                        │
│    ③ daily_sales_summary_* テーブルへUPSERT                │
│         │                                                 │
│         ▼                                                 │
│  JobC: 送信I/F生成・送信                                   │
│    ① daily_sales_summary_* を読み出しファイル生成           │
│    ② SFTP/S3へ配置                                        │
│    ③ 送信完了を示すキックファイルを最後に生成（原子的に）      │
│                                                           │
└───────────────────────────────────────────────────────┘
```

### ジョブ間の連携方式

| 方式 | 説明 | 採用方針 |
|---|---|---|
| **キックファイル方式（推奨）** | 前工程が正常終了した証として空ファイル（`.done`等）を配置し、後続Jobがそれを検知して起動 | 実務で最も枯れたパターン。連携先が別システム（別チーム管理）でも疎結合を保てるため採用する |
| Exit Code連携 | シェルスクリプトやオーケストレータで前JobのExit Codeを見て次Jobを起動 | 同一実行基盤内で完結する場合のみ検討 |
| ジョブスケジューラの依存関係定義 | JP1・AWS Step Functions等でJob間の先行後続関係を定義 | 既存インフラの制約に応じて選択 |

### 実装例：受信フラグの確認（JobA）

```java
@Bean
public Tasklet checkArrivalFlagTasklet(@Value("${batch.input.flag-file}") String flagFile) {
    return (contribution, chunkContext) -> {
        Path flag = Paths.get(flagFile);
        if (!Files.exists(flag)) {
            // リトライハンドラ相当の仕組みで一定間隔リトライし、
            // 一定時間超過でアラート発報して人手介入を促す
            throw new FlagFileNotFoundException("受信I/Fの到着フラグが未検出: " + flagFile);
        }
        return RepeatStatus.FINISHED;
    };
}
```

### 実装例：送信完了フラグの生成（JobC）

書き込み中のファイルを後続処理が誤って読まないよう、**フラグファイルの生成は原子的に行う**。

```java
private void writeCompletionFlag(Path targetDir, String jobDate) throws IOException {
    Path tmp = targetDir.resolve(jobDate + ".done.tmp");
    Path fin = targetDir.resolve(jobDate + ".done");
    Files.createFile(tmp);
    Files.move(tmp, fin, StandardCopyOption.ATOMIC_MOVE); // rename は原子的操作
}
```

---

## 14.4 ジョブ・ステップの分割原則

### 原則：「外部I/O」と「DB内部処理」を同じJob/Stepに混在させない

| 分割理由 | 説明 |
|---|---|
| **リスタート粒度の最適化** | JobC（ファイル送信）だけが失敗した場合、JobB（重い集計処理）をやり直す必要がない。Job単位で `JobRepository` の実行履歴が独立するため、失敗したJobだけ再実行できる |
| **責務の単一化** | JobAは「ネットワーク待ちに強いリトライ」、JobBは「DB負荷を考慮したリトライ」というように、Jobごとにリトライ戦略を個別最適化できる |
| **監視・アラートの粒度** | 「受信I/Fが来ていない」と「集計処理が失敗した」は通知すべき相手（連携先システム担当 vs 自チーム）が異なることが多い。Job単位でアラートを分ける |
| **冪等性の担保しやすさ** | JobCの送信I/F生成だけ再実行したい場合、JobBの集計結果（テーブル）は変更せず読むだけなので安全に再実行できる |

**禁止事項:** 集計処理中に外部システムへのファイル送信も同時に行う設計にしない。DBトランザクションが外部I/Oの完了を待つ形になり、トランザクションが長時間ロックを保持してしまう。

```java
@Bean
public Job dailySalesJobNet(JobRepository repo, Step jobA_intake, Step jobB_aggregate, Step jobC_export) {
    return new JobBuilder("dailySalesJobNet", repo)
        .start(jobA_intake)   // I/O: 外部ファイル取込のみ。DB更新はステージングテーブルへの単純insertのみ
        .next(jobB_aggregate) // DB内部処理のみ。外部I/Oなし。Local Partitioningの対象
        .next(jobC_export)    // 読み取り専用DBアクセス + 外部I/O。DB更新なし
        .build();
}
```

---

## 14.5 分散・並列処理（パーティショニング）

### パーティショニング戦略

| 方式 | 概要 | 採用方針 |
|---|---|---|
| **Local Partitioning** | 同一JVM内でスレッドプールにより並列実行（`TaskExecutorPartitionHandler`） | まずここから開始する。単一インスタンス・CPUコア数に応じたスケールで足りる場合はこれで十分 |
| **Remote Partitioning** | メッセージング基盤経由で複数ワーカーノードに分散 | 単一ノードのCPU/メモリでは処理しきれない規模まで増えた場合に検討する |

**原則:** 過剰設計を避けるため、まずLocal Partitioningで設計し、将来的な規模拡大に備えてパーティションキーの設計だけRemote対応可能な形にしておく。

### パーティションキー設計

顧客IDではなく **`CustomerOrder.id` のレンジ** をパーティションキーとする。顧客IDだと注文の時間的発生ムラの影響を受けやすいため、ID範囲の方が均等な負荷分散になりやすい。

```java
@Bean
public Partitioner orderAggregationPartitioner(CustomerOrderRepository repo) {
    return gridSize -> {
        long maxId = repo.findMaxId();
        long rangeSize = Math.max(maxId / gridSize, 1);
        Map<String, ExecutionContext> partitions = new HashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", i * rangeSize);
            // 最終パーティションは必ずmaxIdまでを含める。
            // (i + 1) * rangeSize を使うと整数除算の切り捨てにより
            // 末尾のID（rangeSize * gridSize より大きい分）が
            // どのパーティションにも属さず取りこぼされる。
            ctx.putLong("maxId", i == gridSize - 1 ? maxId : (i + 1) * rangeSize);
            partitions.put("partition" + i, ctx);
        }
        return partitions;
    };
}
```

### chunkサイズの目安

200万件をchunk 500件で処理すると4,000コミット。ワーカー4並列なら1ワーカーあたり1,000コミットとなる。`default_batch_fetch_size: 100`（[4.7 データアクセス・フェッチ戦略](../../backend/docs/04-entity-design.md)参照）と同様、実測しながらチューニングする。

---

## 14.6 非機能要件

### 性能

**原則:** バッチウィンドウの全時間を処理時間として使い切らない。ウィンドウの60〜70%を正常処理の目標時間とし、残りをI/F待ち・リカバリの予備時間として確保する。

| 項目 | 目標値 | 根拠 |
|---|---|---|
| バッチウィンドウ | 深夜02:00〜05:00の3時間 | 運用要件 |
| 正常処理に使う時間 | 2時間（残り1時間はI/F・リカバリ予備） | ウィンドウの約67% |
| 最低限必要なスループット | 200万件 ÷ 7,200秒 ≈ **278件/秒** | 上記時間からの逆算値。この値はあくまで下限であり設計目標にしない |
| 設計目標スループット | **550〜600件/秒**（安全率2倍） | 劣化時の余白を持たせるため、下限値の2倍を目標に設計する |

**注意:** 逆算した最低ラインをそのまま設計目標にしない。DB負荷増・ネットワーク遅延等で数%劣化しただけでSLA違反になるため、安全率を確保した数値を目標とし、本番の実測で最低ラインを安定して超え続けられることを確認する。

### DBコネクションの分離

バッチ専用のコネクションプール（HikariCP）をオンラインAPI（ec-api本体）とは別設定で用意し、上限を分離する。可能であれば集計処理の読み取りはRead Replicaを使用する。

### 可用性・リカバリ

| 項目 | 方針 |
|---|---|
| 途中失敗時の扱い | `JobRepository` により失敗Stepを記録する。同一 `JobParameters`（処理対象日）で再実行すると失敗箇所から自動再開する |
| リトライ | DB接続断など一時的でリトライ可能な例外は `RetryTemplate` で自動リトライする（例: 3回、指数バックオフ） |
| 二重起動防止 | 同一日のジョブが多重起動しないよう、起動前に `JobExplorer` で実行中ジョブの有無を確認する |
| 部分失敗の扱い | 1ワーカー（1パーティション）が失敗しても他のパーティションは継続する。失敗パーティションのみ再実行対象にする |

### 運用性・監視

| 項目 | 方針 |
|---|---|
| 実行ログ | Job開始/終了、各Stepの処理件数・所要時間を `INFO` ログで出力する（[12.4.4 Service層（手動ログ）](../../backend/docs/12-logging.md)の方針に準拠） |
| 失敗時アラート | Job失敗時に通知する（既存のアラート基盤と連携） |
| 実行履歴の可視化 | `JobRepository` のメタデータテーブル（`BATCH_JOB_EXECUTION`等）を参照できるようにしておく |
| 手動リカバリ手順 | 「失敗時は同じコマンドで再実行すればよい」ことをドキュメント化し、属人化を防ぐ |

### セキュリティ

| 項目 | 方針 |
|---|---|
| 個人情報の扱い | `daily_sales_summary_by_customer` に氏名等は含めず `customer_id` のみ保持する（[12.6 機密情報のマスキング規則](../../backend/docs/12-logging.md)と同じ考え方を集計テーブル設計にも適用する） |
| バッチ実行ユーザー | `AuditorAware` のバッチ用システムユーザー（`id = 0`等）を正式に採用する（[4.2 AuditorAware](../../backend/docs/04-entity-design.md)参照） |

---

## 14.7 Hibernate関連の考慮事項

200万件規模のバッチ処理に、API層（[4章 エンティティ設計](../../backend/docs/04-entity-design.md)）で定めた標準的なJPAの使い方をそのまま適用すると、OOMや性能劣化を確実に引き起こす。バッチのRead/Writeは**API層とは別世界と割り切って設計する**。

### ① 永続化コンテキストの肥大化

通常の `EntityManager` は読み込んだエンティティを一次キャッシュ（永続化コンテキスト）に保持し続けるため、200万件を素朴にループで読むとメモリを食い潰す。

**原則:** バッチのRead/Writeは `StatelessSession` を使用する。通常の `EntityManager` は使わない。

| 項目 | 通常のSession（EntityManager） | StatelessSession |
|---|---|---|
| 一次キャッシュ | あり（肥大化リスク） | **なし**（都度DBアクセス、メモリ安定） |
| 自動Dirty Checking | あり（比較コストが件数に比例して増大） | **なし**（明示的にupdateを呼び出す） |
| カスケード・遅延ロード | 動作する | **動作しない**（意図的に使わない設計にする） |
| 用途 | 通常のCRUD・API層 | **バルク処理専用** |

```java
StatelessSession session = sessionFactory.openStatelessSession();
try {
    ScrollableResults<CustomerOrderDetail> results = session
        .createQuery("SELECT d FROM CustomerOrderDetail d WHERE d.createdAt BETWEEN :from AND :to",
                CustomerOrderDetail.class)
        .setParameter("from", from)
        .setParameter("to", to)
        .scroll(ScrollMode.FORWARD_ONLY); // カーソルで1件ずつ流す。メモリに全件保持しない
    // ...集計...
} finally {
    session.close();
}
```

### ② OFFSETページネーションの罠

`JpaPagingItemReader` のデフォルトはOFFSETベースのページングだが、PostgreSQLはOFFSETが大きくなるほど線形にスキャンコストが増える。200万件の後半になるほど遅くなる。

**原則:** 大量データの読み取りはOFFSETページングではなく**キーセット（シーク）方式**にする。

```sql
-- ❌ 避ける: OFFSET方式（後半になるほど遅い）
SELECT * FROM customer_order_detail ORDER BY id OFFSET 1990000 LIMIT 500;

-- ✅ 推奨: キーセット方式（常に高速）
SELECT * FROM customer_order_detail WHERE id > :lastId ORDER BY id LIMIT 500;
```

`JpaCursorItemReader`、またはキーセットクエリを使う自前 `ItemReader` を使用する。

### ③ hibernate.jdbc.batch_size と Spring Batch の chunk は別レイヤー

| 設定 | 意味 | レイヤー |
|---|---|---|
| Spring Batchの `chunk(500)` | 何件処理したらトランザクションをコミットするか | ビジネス処理の単位 |
| `hibernate.jdbc.batch_size: 50` | 1回のJDBCラウンドトリップで何件のSQL文をまとめて送るか | JDBC通信の最適化 |

**原則:** `order_inserts` / `order_updates` を必ず `true` にする。これがないと異なるエンティティ種別のINSERT/UPDATEが交互に発生した場合にJDBCバッチが分断され、`batch_size` の効果が出ない。

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

### ④ UPSERT はJPAで表現しにくい

`daily_sales_summary_*` への書き込みは「あれば更新、なければ挿入」だが、JPAの `save()` はこれをネイティブにサポートしない。

**原則:** UPSERTが必要な書き込みはJPA経由にせず、`JdbcBatchItemWriter` + 素のJDBCで書く。

**注意:** 14.5節のLocal Partitioningと組み合わせる場合、複数パーティションが同一商品の行を並行して書き込みうる。`total_amount = EXCLUDED.total_amount` のような**置換**ではなく、`total_amount = daily_sales_summary_by_product.total_amount + EXCLUDED.total_amount` の**積算**にしないと、後から書き込んだパーティションが他パーティションの部分集計を上書きしてデータを失う。

```java
@Bean
public JdbcBatchItemWriter<SalesSummaryRow> salesSummaryWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<SalesSummaryRow>()
        .dataSource(dataSource)
        .sql("""
            INSERT INTO daily_sales_summary_by_product
                (product_id, sales_date, total_amount, total_quantity,
                 version, created_by, updated_by, created_at, updated_at)
            VALUES (:productId, :salesDate, :amount, :quantity,
                    0, :systemUserId, :systemUserId, now(), now())
            ON CONFLICT (product_id, sales_date)
            DO UPDATE SET
                total_amount = daily_sales_summary_by_product.total_amount + EXCLUDED.total_amount,
                total_quantity = daily_sales_summary_by_product.total_quantity + EXCLUDED.total_quantity,
                version = daily_sales_summary_by_product.version + 1,
                updated_by = :systemUserId,
                updated_at = now()
            """)
        .itemSqlParameterSourceProvider(row -> {
            var params = new MapSqlParameterSource();
            params.addValue("productId", row.productId());
            params.addValue("salesDate", row.salesDate());
            params.addValue("amount", row.amount());
            params.addValue("quantity", row.quantity());
            params.addValue("systemUserId", BATCH_SYSTEM_USER_ID);
            return params;
        })
        .build();
}
```

これは[4.6 楽観ロック / バルク演算の制限](../../backend/docs/04-entity-design.md)の「バルク演算はversion手動インクリメント必須」というルールをそのまま踏襲したものである。バッチのWriterはエンティティ経由ではないため `AuditorAware` は働かず、`created_by` / `updated_by` にはバッチ用システムユーザーIDを明示的に渡す。

### ⑤ Readerでのリレーション（N+1）に注意

`CustomerOrderDetail` から `Product` 名や `Customer` 情報を都度参照すると、200万件でN+1が発生し致命的になる。

**原則:** 集計処理では極力エンティティのリレーションを辿らず、HQLの `SELECT NEW` かDTO射影で必要なカラムだけを取得する。

**注意:** ①の原則により、この射影クエリも `JpaRepository` の `@Query` メソッド（内部的に通常の`EntityManager`/Sessionを使う）ではなく、`StatelessSession` 経由で発行する。`CustomerOrderDetail` は `product`（`Product`への`@ManyToOne`）と `order`（`CustomerOrder`への`@ManyToOne`、さらに`customer`を保持）というリレーションで商品・顧客とひもづくため、フィールドパスは `d.product.id` / `d.order.customer.id` になる（`d.productId` のようなフラットなカラムは存在しない）。

```java
StatelessSession session = sessionFactory.openStatelessSession();
List<OrderDetailProjection> page = session
    .createQuery(
        """
        SELECT new com.example.ecapi.batch.dto.OrderDetailProjection(
            d.id, d.product.id, d.order.customer.id, d.unitPrice, d.quantity)
        FROM CustomerOrderDetail d
        WHERE d.order.id BETWEEN :minId AND :maxId
          AND d.createdAt BETWEEN :from AND :to
          AND d.id > :lastId
        ORDER BY d.id
        """,
        OrderDetailProjection.class)
    .setParameter("minId", minId)
    .setParameter("maxId", maxId)
    .setParameter("from", from)
    .setParameter("to", to)
    .setParameter("lastId", lastId)
    .setMaxResults(pageSize)
    .list();
```

`minId`/`maxId` は14.5節のパーティションレンジ、`lastId`は②のキーセットページングのカーソルであり、両方の原則を1つのReaderで組み合わせて満たす。`BaseEntity` の監査カラムやLazyな関連の初期化コストを避けられ、200万件規模ではオブジェクト生成コストの差が無視できない。

### まとめ：Hibernate関連ルール一覧

| ルール | 理由 |
|---|---|
| バッチのRead/Writeは `StatelessSession` または素のJDBCを使う。通常の `EntityManager` は使わない | 永続化コンテキスト肥大化・Dirty Checkingコスト回避 |
| 大量データの読み取りはOFFSETページングではなくキーセット方式にする | PostgreSQLでのスキャンコスト増大を回避 |
| `hibernate.jdbc.batch_size` + `order_inserts` / `order_updates` を設定する | JDBCラウンドトリップ削減 |
| UPSERTが必要な書き込みはJPA経由にせず素のJDBCで書く | JPAは `ON CONFLICT` を表現できないため |
| 集計目的のReadはエンティティではなくDTO射影を使う | N+1回避・オブジェクト生成コスト削減 |

---

## 14.8 リスタート・冪等性設計

### JobParametersによる対象範囲の固定

「当日分」の定義を明確にする（例: 前日00:00〜23:59:59に `createdAt` を持つ注文）。バッチ実行中に新規注文が入っても対象範囲がブレないよう、`JobParameters` に集計対象日の開始・終了 `Instant` を固定して渡す。

```java
@Bean
public Job dailyOrderAggregationJob(JobRepository jobRepository, Step masterStep) {
    return new JobBuilder("dailyOrderAggregationJob", jobRepository)
        .start(masterStep)
        .build();
    // restartable はデフォルト true
    // JobParametersに処理対象日を含めることで、
    // 同じ日は再実行時に失敗Stepから自動再開、別日は新規実行として扱われる
}
```

### 冪等性の担保

同じ `JobParameters` で再実行したら同じ結果になることを保証する。Writerは `INSERT` ではなく **UPSERT**（`ON CONFLICT DO UPDATE`）にして、再実行時の重複を防ぐ（[14.7 ④](#④-upsert-はjpaで表現しにくい) 参照）。

### 楽観ロックとの関係

集計処理はあくまで参照系のため `CustomerOrder` 側の `version` には触れない。ただし集計元データが読み取り中に更新されるケース（時刻境界ギリギリの注文）は許容誤差として設計上明記しておく。

---

## 14.9 チェックリスト

### 方式設計時

- [ ] バッチウィンドウに対して安全率（2倍程度）を確保したスループット目標を設定している
- [ ] 逆算した最低ラインの数値をそのまま設計目標にしていない
- [ ] 外部I/Fとの連携がある場合、受信・処理・送信をJobレベルで分離している
- [ ] Job間の連携方式（キックファイル等）を明記している
- [ ] パーティションキーの選定理由を明記している（負荷が均等に分散するか）

### 実装時

- [ ] バッチのRead/Writeで通常の `EntityManager` を使っていない（`StatelessSession` または素のJDBC）
- [ ] 大量データの読み取りにOFFSETページングを使っていない（キーセット方式）
- [ ] `hibernate.jdbc.batch_size` と `order_inserts` / `order_updates` を設定している
- [ ] UPSERTが必要な書き込みをJPAの `save()` で無理に表現していない
- [ ] 集計用の読み取りでエンティティのリレーションを辿っていない（DTO射影を使用）
- [ ] `JobParameters` に処理対象日を含め、再実行時の挙動を制御している
- [ ] `created_by` / `updated_by` にバッチ専用システムユーザーIDを明示的に設定している

### 運用設計時

- [ ] Job失敗時のアラート通知先を明記している
- [ ] 同一日のジョブ多重起動を防止する仕組みがある
- [ ] 手動リカバリ手順をドキュメント化している
- [ ] バッチ専用のDBコネクションプールをオンラインAPIと分離している
