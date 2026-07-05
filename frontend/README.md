# EC サイト フロントエンド

Next.js (App Router) + TypeScript で実装した EC サイトのフロントエンドです。バックエンド API(`backend/`、Spring Boot)を呼び出して動作します。

## 技術スタック

- [Next.js](https://nextjs.org) 16 (App Router)
- React 19 / TypeScript 5
- [openapi-typescript](https://openapi-ts.dev/) — バックエンドの OpenAPI 定義から API 型を自動生成

## セットアップ

1. 依存関係をインストールします。

   ```bash
   npm install
   ```

2. 環境変数を設定します(`.env.local.example` をコピー)。

   ```bash
   cp .env.local.example .env.local
   ```

   | 変数                       | 説明                          | デフォルト              |
   | -------------------------- | ----------------------------- | ----------------------- |
   | `NEXT_PUBLIC_API_BASE_URL` | バックエンド API のベース URL | `http://localhost:8080` |

3. バックエンド API を起動した上で、開発サーバーを起動します。

   ```bash
   npm run dev
   ```

   [http://localhost:3000](http://localhost:3000) をブラウザで開きます。

## npm スクリプト

| コマンド                     | 説明                                                |
| ---------------------------- | --------------------------------------------------- |
| `npm run dev`                | 開発サーバーを起動                                  |
| `npm run build`              | 本番ビルド                                          |
| `npm run start`              | 本番ビルドを起動                                    |
| `npm run lint`               | ESLint を実行                                       |
| `npm run format`             | Prettier で全ファイルを整形                         |
| `npm run format:check`       | 整形されているかチェック(CI 向け・ファイル変更なし) |
| `npm run generate:api-types` | OpenAPI 定義から `src/lib/api/schema.d.ts` を再生成 |

`generate:api-types` はバックエンド起動中に実行します(既定で `http://localhost:8080/v3/api-docs` を参照。`API_DOCS_URL` で変更可)。バックエンドの API を変更したら再生成してください。

## 画面構成

顧客向けと管理者向け(`/admin` 配下)の2系統に分かれています。`src/app/` のディレクトリ構成がそのまま URL になります。

### 顧客向け

| パス             | 画面                                                     |
| ---------------- | -------------------------------------------------------- |
| `/`              | トップページ(ユーザーログイン・管理者ログインへのリンク) |
| `/login`         | 顧客ログイン                                             |
| `/products`      | 商品一覧                                                 |
| `/products/[id]` | 商品詳細(「カートに追加」ボタン付き)                     |
| `/cart`          | カート                                                   |
| `/orders`        | 注文履歴                                                 |
| `/orders/[id]`   | 注文詳細                                                 |
| `/mypage`        | マイページ                                               |

### 管理者向け

| パス                                 | 画面                           |
| ------------------------------------ | ------------------------------ |
| `/admin/login`                       | 管理者ログイン                 |
| `/admin`                             | 管理トップ                     |
| `/admin/products`、`/new`、`/[id]`   | 商品管理(一覧・登録・編集)     |
| `/admin/categories`、`/new`、`/[id]` | カテゴリ管理(一覧・登録・編集) |
| `/admin/customers`、`/[id]`          | 顧客管理(一覧・詳細)           |
| `/admin/employees`、`/new`、`/[id]`  | 従業員管理(一覧・登録・編集)   |
| `/admin/orders`、`/[id]`             | 注文管理(一覧・詳細)           |

管理画面は `src/app/admin/layout.tsx` の共通レイアウトで囲まれており、上部ナビ(管理トップ/商品管理/カテゴリ管理/顧客管理/従業員管理/注文管理/ログアウト)が表示されます。`/admin/login` のみナビなしで表示されます。

## ディレクトリ構成

```
src/
├── app/            # App Router のページ(顧客向け + admin/ 配下に管理画面)
└── lib/api/        # バックエンド API クライアント
    ├── client.ts   # fetch ラッパー(apiFetch / ApiError)
    ├── schema.d.ts # OpenAPI から自動生成した型定義(手動編集しない)
    ├── auth.ts / customerAuth.ts   # 管理者 / 顧客の認証
    ├── products.ts / cart.ts / orders.ts / me.ts  # 顧客向け API
    └── admin*.ts   # 管理者向け API(商品・カテゴリ・注文・顧客・従業員)
```

## API 通信

- すべてのリクエストは `src/lib/api/client.ts` の `apiFetch` を経由します。
- 認証は Cookie ベースのセッションで、`credentials: "include"` を付けて送信します(管理者: `/api/auth/*`、顧客: `/api/customer/auth/*`)。
- エラー時は `ApiError`(HTTP ステータス + レスポンスボディ)が throw されます。
