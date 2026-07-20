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
| `com.example.ecapi.batch.config` | `BatchAuditConfig`（バッチ専用`AuditorAware`） |
| `com.example.ecapi.batch.job` | Job/Step定義、Partitioner、Reader、Processor、Job固有の例外、`JobParametersProvider`実装（例: `DailySalesJobParametersProvider`） |
| `com.example.ecapi.batch.writer` | ステージングテーブル・最終テーブルへの`JdbcBatchItemWriter`設定 |
| `com.example.ecapi.batch.dto` | Reader/Writer間のDTO射影 |

## 起動フロー

```
java -jar app.jar [--job=jobBeanName] [--targetDate=YYYY-MM-DD]
  │
  ▼
BatchApplication.main()
  │  SpringApplication.run() でコンテキスト起動
  │  spring.batch.job.enabled=false のため標準のJobLauncherApplicationRunnerは無効
  ▼
BatchRunner.run()  ← CommandLineRunnerとして自動実行
  │
  ├─ --job未指定時はdailySalesAggregationJobをデフォルトで選択（既存運用のECS RunTaskは引数なしで起動するため後方互換）
  ├─ jobRepository.findRunningJobExecutions(...) で二重起動を検知し、実行中なら中止
  ├─ 選択したJobに対応する JobParametersProvider.resolve(args) でJobParametersを組み立てて jobOperator.start(...)
  │     dailySalesAggregationJobの場合: --targetDate未指定時は「前日」（JST基準）を対象日とし、targetDateFrom/targetDateToを積む
  │     JobA(受信フラグ確認) → JobB(集計: Local Partitioning→Consolidate) → JobC(完了フラグ生成)
  │       JobB-Worker: ステージングテーブルへ明細のまま単純INSERT（fault tolerance: データ不正はskip、一時的なDBエラーはretry）
  │       JobB-Consolidate: chunk(1000)構成。job_instance_id単位でステージングをGROUP BY/SUMしながら
  │                         カーソルで読み、最終テーブルへ1行ずつ置換UPSERT。全chunk完了後（成功時のみ）
  │                         StagingCleanupListenerがステージング行をまとめてDELETE
  └─ JobExecutionの結果をexit codeに反映（ExitCodeGenerator）
  ▼
BatchApplication.main() に戻り System.exit(SpringApplication.exit(context))
  → 失敗時はexit code 1でプロセス終了（ECS RunTask/CodeBuildが検知できるように）
```

同じ`--targetDate`で再実行すると同一`JobInstance`とみなされ、失敗Stepから自動再開する（`JobRepository`のリスタート機能、[docs/batch.md 14.8](docs/batch.md#148-リスタート冪等性設計)参照）。

`--job`にはJob Beanの名前（例: `dailySalesAggregationJob`）を指定する。存在しない名前を指定した場合は起動時に`IllegalArgumentException`で落ちる。

日次売上集計以外のJobを追加する場合、`BatchRunner`自体は変更不要で、以下の2つを追加するだけでよい。

- 新しい`@Bean Job`（Beanは自動的に`Map<String, Job>`として`BatchRunner`に注入される）
- そのJob専用の`JobParametersProvider`実装（`jobName()`が対応するJob Bean名を返すこと。`--job`で選択されたJobの起動時に、そのJobParametersProviderの`resolve(args)`でJobParametersを組み立てる。実装が存在しないJobを起動しようとすると`IllegalStateException`で落ちる）

JobParametersの形はJob毎に異なってよい（`DailySalesJobParametersProvider`はtargetDateFrom/targetDateToを組み立てるが、他のJobが全く別のパラメータ形状を必要としても`BatchRunner`側の変更は不要）。

## ローカルでの実行

`backend/docker-compose.yml`のPostgresを共用する。コマンドはすべて**リポジトリルート**（`ec-api/`。Gradleマルチモジュールのルート）から実行する。

```bash
docker compose -f backend/docker-compose.yml up -d

mkdir -p batch/tmp/batch/input
touch batch/tmp/batch/input/payment_confirmed_20240115.done  # JobAの受信フラグ（対象日入り。事前に手動で用意）

SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_PASSWORD=changeme \
  ./gradlew :batch:bootRun --args='--targetDate=2024-01-15'
```

`local`プロファイルでは受信フラグ・送信出力とも`batch/tmp/batch/`配下（`application-local.yml`）を見る。受信フラグのファイル名は`batch.input.flag-file-template`（`%s`が対象日の`yyyyMMdd`）で組み立てる。

fault toleranceでskipされたレコードは`batch_skipped_records`テーブルに記録される。

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
