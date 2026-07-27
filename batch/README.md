# ec-api バッチ

日次売上集計バッチ（Spring Batch）。API（`backend/`）とはソース・設定・デプロイを分離した独立モジュール。設計方針・規約は [`docs/batch.md`](docs/batch.md) を参照。

## 技術スタック

| 分類 | 内容 |
|---|---|
| フレームワーク | Spring Boot 4.0.1 / Spring Batch |
| ORM | Hibernate 7.1（`StatelessSession`を集計処理で使用） |
| DB | PostgreSQL（`backend`と同一DBインスタンス、コネクションプールは別設定） |
| 言語 | Java 25 |
| ビルド | Gradle（マルチモジュールの一部。ルートに`settings.gradle.kts`） |

## モジュール構成

`ec-api`直下のGradleマルチモジュール構成の1つ。`entity`/`repository`は`core`モジュールを共有依存し、`backend`と`batch`は互いに依存しない。

```
ec-api/
├── core/     … entity・repository（backendとbatchが共有）
├── backend/  … API
└── batch/    … このモジュール
```

## パッケージ構成

| パッケージ | 内容 |
|---|---|
| `com.example.ecapi.batch` | `BatchApplication`（エントリポイント）、`BatchRunner`（起動経路）、`JobParametersProvider`（Job毎のJobParameters組み立ての拡張点） |
| `com.example.ecapi.batch.config` | `BatchAuditConfig`（バッチ専用`AuditorAware`）、`BatchJdbcJobRepositoryConfig`（`@EnableJdbcJobRepository`によるJDBC永続化ジョブリポジトリの明示的な有効化） |
| `com.example.ecapi.batch.job.{jobName}` | Job単位のサブパッケージ。そのJobのJob/Step定義、Partitioner、Processor、`ItemStreamReader`/`JdbcBatchItemWriter`実装、固有の例外・リスナー、`JobParametersProvider`実装をまとめる（Reader/Writerも含め、単一Jobからしか参照されないものはすべてそのJobのパッケージに置く）。日次売上集計ジョブネット（元は単一Jobだったが、リスタート・監視をJob単位で独立させるため3Jobに分割）は`job.paymentintake`（`PaymentIntakeJobConfig`・`PaymentIntakeJobParametersProvider`、`PaymentReconciliationItemReader`、`PaymentConfirmationWriterConfig`/`PaymentUpsertWriterConfig`、決済突合まわりの`PaymentReconciliationItemProcessor`/`PaymentReconciliationSkipListener`/`PaymentReconciliationAlertJobListener`等）、`job.salesaggregation`（`SalesAggregationJobConfig`・`SalesAggregationJobParametersProvider`、`OrderDetailKeysetItemReader`/`StagingAggregateItemReader`、`SalesSummaryWriterConfig`、`OrderAggregationPartitioner`等）、`job.settlementexport`（`SettlementExportJobConfig`・`SettlementExportJobParametersProvider`、`PaymentSettlementKeysetItemReader`等）の3パッケージで構成される。新規Job追加時は同様に`job.{jobName}`を切る |
| `com.example.ecapi.batch.job.dailysales` | 上記3Jobをまたいで参照される共通ロジックのみを置く（特定のJobに属さないため`job.{jobName}`の対象外）。`TargetDateRangeJobParameters`（3Jobの`JobParametersProvider`が共有するtargetDateFrom/targetDateTo算出）、`TargetDateFormatter`（`job.paymentintake`/`job.settlementexport`が共有するyyyyMMdd変換） |
| `com.example.ecapi.batch.dto` | Reader/Writer間のDTO射影 |

## 起動フロー

日次売上集計は「受信I/F取込」「売上集計」「決済明細出力」という性質の異なる3つの業務ドメインを持つため、`paymentIntakeJob`・`salesAggregationJob`・`settlementExportJob`の3Jobに分かれている。3Jobの実行順序（`paymentIntakeJob` → `salesAggregationJob` → `settlementExportJob`）はSpring Batch自体には持たせず、外部のオーケストレーション層（スケジューラ、または個別プロセスの逐次起動）が各プロセスの終了コード（0/1）を見て担う。

```
java -jar app.jar --job=jobBeanName [--targetDate=YYYY-MM-DD]
  │
  ▼
BatchApplication.main()
  │  SpringApplication.run() でコンテキスト起動
  │  spring.batch.job.enabled=false のため標準のJobLauncherApplicationRunnerは無効
  ▼
BatchRunner.run()  ← CommandLineRunnerとして自動実行
  │
  ├─ --jobは必須。未指定または未知のJob名の場合はIllegalArgumentExceptionで起動を中止する
  ├─ jobRepository.findRunningJobExecutions(...) で二重起動を検知し、実行中なら中止
  ├─ 選択したJobに対応する JobParametersProvider.resolve(args) でJobParametersを組み立てて jobOperator.start(...)
  │     3Jobとも --targetDate未指定時は「前日」（JST基準）を対象日とし、targetDateFrom/targetDateToを積む
  │     （算出ロジックはTargetDateRangeJobParametersに共通化、jobName()の返り値だけが異なる薄いラッパーとして
  │      PaymentIntakeJobParametersProvider/SalesAggregationJobParametersProvider/SettlementExportJobParametersProviderが実装）
  │
  │     --job=paymentIntakeJob: 取込フェーズ(受信フラグ確認→受信CSV検証・ステージング取込)
  │       → 決済突合フェーズ(ステージング→customer_order突合→paymentへUPSERT)
  │       決済突合フェーズ: chunk(500)構成。ステージングをcustomer_orderに突合しpaymentへUPSERT
  │                         （fault tolerance: 孤立レコード・未知statusはskipしpayment_reconciliation_alertsへ記録、
  │                          一時的なDBエラーはretry）。Job完了後PaymentReconciliationAlertJobListenerが
  │                          当該jobExecutionIdのアラート件数を確認し、1件以上あればExitStatusを
  │                          PARTIAL_SUCCESS_WITH_ALERTSにしてCOMPLETEDと区別する（このリスナーはpaymentIntakeJobのみに登録）
  │
  │     --job=salesAggregationJob: 集計フェーズ(集計: Local Partitioning→Consolidate)
  │       集計フェーズ-Worker: ステージングテーブルへ明細のまま単純INSERT（fault tolerance: 一時的なDBエラーのみretryで区別し、
  │                         データ不正はskipせず即座にStepを異常終了させる。paymentIntakeJobの後続であり
  │                         決済データの不正は既に排除されている前提のため、想定外のデータはアサーション違反として扱う）
  │       集計フェーズ-Consolidate: chunk(1000)構成。job_instance_id単位でステージングをGROUP BY/SUMしながら
  │                         カーソルで読み、最終テーブルへ1行ずつ置換UPSERT。全chunk完了後（成功時のみ）
  │                         StagingCleanupListenerがステージング行をまとめてDELETE
  │
  │     --job=settlementExportJob: 送信フェーズ(決済明細ファイル生成→決済明細フラグ生成→完了フラグ生成)
  │       送信フェーズ: PAYMENTテーブル（status = CAPTURED）から決済明細CSV（入金消込用）を直接抽出・出力した後、
  │                     専用の完了フラグを生成してから、ジョブネット全体の完了を示すフラグを生成する
  │                     （CSV生成とフラグ生成を別Stepに分け、リスタート時に重いI/Oをやり直さないため。
  │                      完了フラグは3Jobのうち最後に実行される想定のこのJobの末尾に置く）
  └─ JobExecutionの結果をexit codeに反映（ExitCodeGenerator）
  ▼
BatchApplication.main() に戻り System.exit(SpringApplication.exit(context))
  → 失敗時はexit code 1でプロセス終了（呼び出し元のオーケストレーション層が後続Jobの起動可否を判断できるように）
```

同じ`--job`・`--targetDate`で再実行すると同一`JobInstance`とみなされ、失敗Stepから自動再開する（`JobRepository`のリスタート機能、[docs/batch.md 14.8](docs/batch.md#148-リスタート冪等性設計)参照）。3Jobは独立した`JobInstance`系列を持つため、例えば決済明細出力（`settlementExportJob`）だけを個別に再実行するといったことができる。

`--job`にはJob Beanの名前（`paymentIntakeJob`/`salesAggregationJob`/`settlementExportJob`のいずれか）を指定する。**必須引数であり、未指定の場合も存在しない名前を指定した場合と同様に起動時に`IllegalArgumentException`で落ちる**（「引数なし起動で何が起動するか」を暗黙のデフォルトに委ねると事故りやすいため、後方互換のデフォルトJobは持たない）。

> **運用への影響:** 分割前はEventBridge Schedulerが`ecs:RunTask`を引数なしで呼び出す1本の設定で足りていたが、分割後は3回（`--job=paymentIntakeJob`→`--job=salesAggregationJob`→`--job=settlementExportJob`の順に、前段の終了コードが0であることを確認しながら）呼び出す構成へ変更する必要がある。このオーケストレーション層（`infrastructure/terraform`のEventBridge Scheduler設定等）の更新は本変更のスコープ外であり、別途対応が必要。

新しいJobを追加する場合、`BatchRunner`自体は変更不要で、以下の2つを追加するだけでよい。

- 新しい`@Bean Job`（Beanは自動的に`Map<String, Job>`として`BatchRunner`に注入される）
- そのJob専用の`JobParametersProvider`実装（`jobName()`が対応するJob Bean名を返すこと。`--job`で選択されたJobの起動時に、そのJobParametersProviderの`resolve(args)`でJobParametersを組み立てる。実装が存在しないJobを起動しようとすると`IllegalStateException`で落ちる）

JobParametersの形はJob毎に異なってよい（日次売上集計ジョブネットの3Jobはいずれも`TargetDateRangeJobParameters`経由でtargetDateFrom/targetDateToを組み立てるが、他のJobが全く別のパラメータ形状を必要としても`BatchRunner`側の変更は不要）。

## ローカルでの実行

`backend/docker-compose.yml`のPostgresを共用する。コマンドはすべて**リポジトリルート**（`ec-api/`。Gradleマルチモジュールのルート）から実行する。

```bash
docker compose -f backend/docker-compose.yml up -d

mkdir -p batch/tmp/batch/input

# 取込フェーズが取り込む決済確定明細CSV（対象日入り。事前に手動で用意）
# order_numberはcustomer_order.order_number（外部連携用の参照番号、内部idではない）と対応させる
cat <<'CSV' > batch/tmp/batch/input/payment_confirmed_20240115.csv
order_number,transaction_id,customer_id,payment_method,status,amount,fee,settled_at
3fa85f64-5717-4562-b3fc-2c963f66afa6,txn_8f3c1a2b9d4e,1,CREDIT_CARD,SETTLED,12800.00,384.00,2024-01-15T03:12:45Z
CSV

touch batch/tmp/batch/input/payment_confirmed_20240115.done  # 取込フェーズの受信フラグ（CSVの書き込み完了を示すキックファイル）

# 3Jobを順番に実行する（前段が失敗した場合は後段を実行しない）
SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_PASSWORD=password \
  ./gradlew :batch:bootRun --args='--job=paymentIntakeJob --targetDate=2024-01-15'
SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_PASSWORD=password \
  ./gradlew :batch:bootRun --args='--job=salesAggregationJob --targetDate=2024-01-15'
SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_PASSWORD=password \
  ./gradlew :batch:bootRun --args='--job=settlementExportJob --targetDate=2024-01-15'
```

`local`プロファイルでは受信フラグ・受信CSV・送信出力とも`batch/tmp/batch/`配下（`application-local.yml`）を見る。ファイル名は`batch.input.flag-file-template`/`batch.input.data-file-template`（`%s`が対象日の`yyyyMMdd`）で組み立てる。受信CSVはヘッダー`order_number,transaction_id,customer_id,payment_method,status,amount,fee,settled_at`固定で、フォーマット不正（ヘッダー不一致・数値/日時としてパースできない値）は取込フェーズを異常終了させる（集計フェーズのデータ不正のようなskipはしない）。`order_number`は内部サロゲートキー（`customer_order.id`）ではなく、決済代行が実際に知り得る外部連携用の参照番号（`customer_order.order_number`）を使う。`customer_id`は突合・監査用の識別子のみで、氏名・住所等のPIIは含めない。取り込んだ内容は`payment_confirmation_staging`テーブルに保存され、後続の決済突合フェーズ（`paymentReconciliationStep`）がこのテーブルを`customer_order`にLEFT JOINして読み出す。詳細は`docs/batch.md`の14.3節を参照。

送信フェーズでは、`daily_sales_summary_by_product`（商品単位の集計値。`payment_id`・`fee`・`net_amount`を持たない）とは別に、`payment`テーブル（`status = CAPTURED`）から直接抽出した決済明細CSV（`settlement_detail_YYYYmmdd.csv`、入金消込用）が出力ディレクトリ（`batch.output.dir`）に書き出される。

fault toleranceでskipされたレコードは、決済突合フェーズ（`paymentReconciliationStep`）分のみ`payment_reconciliation_alerts`テーブルに記録される。1件以上ある場合、`PaymentReconciliationAlertJobListener`がJobのExitStatusを`PARTIAL_SUCCESS_WITH_ALERTS`にし、正常完了（`COMPLETED`）と区別できるようにする（外部通知先は未確定のため、現時点ではWARNログ出力とExitStatusの区別まで）。集計フェーズ（`salesAggregateWorkerStep`）はpaymentIntakeJobの後続であり決済データの不正は既に排除されている前提のため、データ不正はskip対象にせず即座にStepを異常終了させる（`batch_skipped_records`テーブルへの記録は行わない）。

## ビルド・テスト

```bash
# リポジトリルートで
./gradlew :batch:build
./gradlew :batch:test
```

## Docker

ビルドコンテキストはリポジトリルート（`core`を共有するため）。

```bash
docker build -f batch/Dockerfile -t ec-api-batch .
```

## デプロイ

APIとは独立したECRリポジトリ・ECSタスク定義・CodePipelineを持つ（`infrastructure/terraform`）。常駐サービスではなく、EventBridge Schedulerが日次（デフォルト JST 02:00）で`ecs:RunTask`を呼び出す使い捨てタスク。デプロイは`batch/buildspec.yml`内でイメージのビルド・ECR push後、`aws ecs register-task-definition`で新リビジョンを登録して完結する（ECS Serviceのローリングアップデートは行わない）。

詳細な設計判断（パーティショニング、UPSERT戦略、リスタート設計など）は [`docs/batch.md`](docs/batch.md) を参照。
