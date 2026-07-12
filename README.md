# EC サイト (ec-api)

Spring Boot (backend) + Next.js (frontend) で構成された EC サイトのサンプルプロジェクトです。

- `backend/` — Spring Boot API([詳細README](backend/README.md))
- `frontend/` — Next.js フロントエンド([詳細README](frontend/README.md))
- `infrastructure/` — Terraform (AWS: VPC / ECR / RDS / ALB / ECS / CodePipeline)

## 前提ツール

| ツール | バージョン目安 |
|--------|----------------|
| Docker / docker-compose | 最新 |
| Java | 25 |
| Node.js | 20 以上 |
| npm | Node.js に同梱のもの |

## セットアップ手順

### 1. リポジトリを取得

```bash
git clone <このリポジトリのURL>
cd ec-api
```

### 2. backend を起動

```bash
cd backend

# DB(PostgreSQL) と Redis を起動
docker-compose up -d
```

`src/main/resources/application.yml` の DB ユーザー名・パスワードを環境に合わせて確認・変更してください。

```bash
./gradlew bootRun
```

- 起動時にテーブルが自動作成され、サンプル商品5件が投入されます。
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 動作確認用のログイン: `admin@example.com` / `password`

### 3. frontend を起動

別ターミナルで:

```bash
cd frontend
npm install

# 環境変数ファイルを作成(デフォルトのままで backend: http://localhost:8080 を参照)
cp .env.local.example .env.local

npm run dev
```

http://localhost:3000 をブラウザで開きます。

### 4. バックエンドの API を変更したとき

frontend 側の型定義(`src/lib/api/schema.d.ts`)を再生成します(backend 起動中に実行)。

```bash
cd frontend
npm run generate:api-types
```

## よくある詰まりポイント

| 症状 | 原因・対処 |
|------|-----------|
| `docker-compose up` でポート競合エラー | 5432(PostgreSQL)/6379(Redis)がローカルで既に使われていないか確認 |
| frontend から API が呼べない | `.env.local` の `NEXT_PUBLIC_API_BASE_URL` が backend の起動先(既定 `http://localhost:8080`)と一致しているか確認 |
| API のレスポンス型がずれる/コンパイルエラー | backend の API を変更した後に `npm run generate:api-types` を実行し忘れていないか確認 |
| ログインできない | サンプルデータ投入直後は `admin@example.com` / `password`(管理者)を使用。DB を作り直した場合は再投入されているか確認 |
| 商品更新(PUT)が失敗する | 楽観ロック用の `version` をリクエストボディに含める必要あり(詳細は backend/README.md 参照) |

## CI/CD

- GitHub Actions(`.github/workflows/ci.yml`): push 時に Spotless チェック・テストを実行
- CodePipeline: GitHub → CodeBuild(Docker build → ECR push)→ ECS Run Task(Flyway でマイグレーション)→ ECS Fargate デプロイ

インフラの構築手順(Terraform)は `infrastructure/terraform` 配下、および `backend/README.md` の「terraform実行手順」を参照してください。

## 詳細情報

- API エンドポイント一覧・curl での動作確認例 → [backend/README.md](backend/README.md)
- 画面構成・ディレクトリ構成・API 通信の仕組み → [frontend/README.md](frontend/README.md)